/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder

import java.util.Collections
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.JetType
import kotlin.properties.Delegates
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.idea.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.refactoring.EmptyValidator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.util.supertypes
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassInfo
import org.jetbrains.kotlin.idea.util.makeNotNullable

/**
 * Represents a concrete type or a set of types yet to be inferred from an expression.
 */
abstract class TypeInfo(val variance: Variance) {
    object Empty: TypeInfo(Variance.INVARIANT) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> = Collections.emptyList()
    }

    class ByExpression(val expression: JetExpression, variance: Variance): TypeInfo(variance) {
        override val possibleNamesFromExpression: Array<String> by Delegates.lazy {
            JetNameSuggester.suggestNamesForExpression(expression, EmptyValidator)
        }

        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                expression.guessTypes(builder.currentFileContext, builder.currentFileModule).flatMap { it.getPossibleSupertypes(variance) }
    }

    class ByTypeReference(val typeReference: JetTypeReference, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                builder.currentFileContext[BindingContext.TYPE, typeReference].getPossibleSupertypes(variance)
    }

    class ByType(val theType: JetType, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                theType.getPossibleSupertypes(variance)
    }

    class ByReceiverType(variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                (builder.placement as CallablePlacement.WithReceiver).receiverTypeCandidate.theType.getPossibleSupertypes(variance)
    }

    abstract class DelegatingTypeInfo(val delegate: TypeInfo): TypeInfo(delegate.variance) {
        override val substitutionsAllowed: Boolean = delegate.substitutionsAllowed
        override val possibleNamesFromExpression: Array<String> get() = delegate.possibleNamesFromExpression
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> = delegate.getPossibleTypes(builder)
    }

    class NoSubstitutions(delegate: TypeInfo): DelegatingTypeInfo(delegate) {
        override val substitutionsAllowed: Boolean = false
    }

    class StaticContextRequired(delegate: TypeInfo): DelegatingTypeInfo(delegate) {
        override val staticContextRequired: Boolean = true
    }

    open val substitutionsAllowed: Boolean = true
    open val staticContextRequired: Boolean = false
    open val possibleNamesFromExpression: Array<String> get() = ArrayUtil.EMPTY_STRING_ARRAY
    abstract fun getPossibleTypes(builder: CallableBuilder): List<JetType>

    protected fun JetType?.getPossibleSupertypes(variance: Variance): List<JetType> {
        if (this == null || ErrorUtils.containsErrorType(this)) return Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType())
        val single = Collections.singletonList(this)
        return when (variance) {
            Variance.IN_VARIANCE -> single + supertypes()
            else -> single
        }
    }
}

fun TypeInfo(expressionOfType: JetExpression, variance: Variance): TypeInfo = TypeInfo.ByExpression(expressionOfType, variance)
fun TypeInfo(typeReference: JetTypeReference, variance: Variance): TypeInfo = TypeInfo.ByTypeReference(typeReference, variance)
fun TypeInfo(theType: JetType, variance: Variance): TypeInfo = TypeInfo.ByType(theType, variance)

fun TypeInfo.noSubstitutions(): TypeInfo = (this as? TypeInfo.NoSubstitutions) ?: TypeInfo.NoSubstitutions(this)

fun TypeInfo.forceNotNull(): TypeInfo {
    class ForcedNotNull(delegate: TypeInfo): TypeInfo.DelegatingTypeInfo(delegate) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                super.getPossibleTypes(builder).map { it.makeNotNullable() }
    }

    return (this as? ForcedNotNull) ?: ForcedNotNull(this)
}

/**
 * Encapsulates information about a function parameter that is going to be created.
 */
class ParameterInfo(
        val typeInfo: TypeInfo,
        val preferredName: String? = null
)

enum class CallableKind {
    FUNCTION
    CONSTRUCTOR
    PROPERTY
}

abstract class CallableInfo (
        val name: String,
        val receiverTypeInfo: TypeInfo,
        val returnTypeInfo: TypeInfo,
        val possibleContainers: List<JetElement>,
        val typeParameterInfos: List<TypeInfo>
) {
    abstract val kind: CallableKind
    abstract val parameterInfos: List<ParameterInfo>
}

class FunctionInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   possibleContainers: List<JetElement> = Collections.emptyList(),
                   override val parameterInfos: List<ParameterInfo> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList()
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos) {
    override val kind: CallableKind get() = CallableKind.FUNCTION
}

class ConstructorInfo(val classInfo: ClassInfo, expectedTypeInfo: TypeInfo): CallableInfo(
        classInfo.name, TypeInfo.Empty, expectedTypeInfo.forceNotNull(), Collections.emptyList(), classInfo.typeArguments
) {
    override val kind: CallableKind get() = CallableKind.CONSTRUCTOR
    override val parameterInfos: List<ParameterInfo> get() = classInfo.parameterInfos
}

class PropertyInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   val writable: Boolean,
                   possibleContainers: List<JetElement> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList()
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos) {
    override val kind: CallableKind get() = CallableKind.PROPERTY
    override val parameterInfos: List<ParameterInfo> get() = Collections.emptyList()
}
