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

import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassInfo
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.util.*

/**
 * Represents a concrete type or a set of types yet to be inferred from an expression.
 */
abstract class TypeInfo(val variance: Variance) {
    object Empty: TypeInfo(Variance.INVARIANT) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> = Collections.emptyList()
    }

    class ByExpression(val expression: KtExpression, variance: Variance): TypeInfo(variance) {
        override val possibleNamesFromExpression: Array<String> by lazy {
            KotlinNameSuggester.suggestNamesByExpressionOnly(expression, { true }).toTypedArray()
        }

        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> =
                expression.guessTypes(
                        context = builder.currentFileContext,
                        module = builder.currentFileModule,
                        pseudocode = builder.pseudocode
                ).flatMap { it.getPossibleSupertypes(variance, builder) }
    }

    class ByTypeReference(val typeReference: KtTypeReference, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> =
                builder.currentFileContext[BindingContext.TYPE, typeReference].getPossibleSupertypes(variance, builder)
    }

    class ByType(val theType: KtType, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> =
                theType.getPossibleSupertypes(variance, builder)
    }

    class ByReceiverType(variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> =
                (builder.placement as CallablePlacement.WithReceiver).receiverTypeCandidate.theType.getPossibleSupertypes(variance, builder)
    }

    abstract class DelegatingTypeInfo(val delegate: TypeInfo): TypeInfo(delegate.variance) {
        override val substitutionsAllowed: Boolean = delegate.substitutionsAllowed
        override val possibleNamesFromExpression: Array<String> get() = delegate.possibleNamesFromExpression
        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> = delegate.getPossibleTypes(builder)
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
    abstract fun getPossibleTypes(builder: CallableBuilder): List<KtType>

    protected fun KtType?.getPossibleSupertypes(variance: Variance, callableBuilder: CallableBuilder): List<KtType> {
        if (this == null || ErrorUtils.containsErrorType(this)) {
            return Collections.singletonList(callableBuilder.currentFileModule.builtIns.anyType)
        }
        val single = Collections.singletonList(this)
        return when (variance) {
            Variance.IN_VARIANCE -> single + supertypes()
            else -> single
        }
    }
}

fun TypeInfo(expressionOfType: KtExpression, variance: Variance): TypeInfo = TypeInfo.ByExpression(expressionOfType, variance)
fun TypeInfo(typeReference: KtTypeReference, variance: Variance): TypeInfo = TypeInfo.ByTypeReference(typeReference, variance)
fun TypeInfo(theType: KtType, variance: Variance): TypeInfo = TypeInfo.ByType(theType, variance)

fun TypeInfo.noSubstitutions(): TypeInfo = (this as? TypeInfo.NoSubstitutions) ?: TypeInfo.NoSubstitutions(this)

fun TypeInfo.forceNotNull(): TypeInfo {
    class ForcedNotNull(delegate: TypeInfo): TypeInfo.DelegatingTypeInfo(delegate) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KtType> =
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
    FUNCTION,
    CLASS_WITH_PRIMARY_CONSTRUCTOR,
    SECONDARY_CONSTRUCTOR,
    PROPERTY
}

abstract class CallableInfo (
        val name: String,
        val receiverTypeInfo: TypeInfo,
        val returnTypeInfo: TypeInfo,
        val possibleContainers: List<KtElement>,
        val typeParameterInfos: List<TypeInfo>
) {
    abstract val kind: CallableKind
    abstract val parameterInfos: List<ParameterInfo>
}

class FunctionInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   possibleContainers: List<KtElement> = Collections.emptyList(),
                   override val parameterInfos: List<ParameterInfo> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList(),
                   val isOperator: Boolean = false
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos) {
    override val kind: CallableKind get() = CallableKind.FUNCTION
}

class PrimaryConstructorInfo(val classInfo: ClassInfo, expectedTypeInfo: TypeInfo): CallableInfo(
        classInfo.name, TypeInfo.Empty, expectedTypeInfo.forceNotNull(), Collections.emptyList(), classInfo.typeArguments
) {
    override val kind: CallableKind get() = CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR
    override val parameterInfos: List<ParameterInfo> get() = classInfo.parameterInfos
}

class SecondaryConstructorInfo(
        override val parameterInfos: List<ParameterInfo>,
        val targetClass: PsiElement
): CallableInfo("", TypeInfo.Empty, TypeInfo.Empty, Collections.emptyList(), Collections.emptyList()) {
    override val kind: CallableKind get() = CallableKind.SECONDARY_CONSTRUCTOR
}

class PropertyInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   val writable: Boolean,
                   possibleContainers: List<KtElement> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList()
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos) {
    override val kind: CallableKind get() = CallableKind.PROPERTY
    override val parameterInfos: List<ParameterInfo> get() = Collections.emptyList()
}
