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
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassInfo
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.getResolvableApproximations
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

/**
 * Represents a concrete type or a set of types yet to be inferred from an expression.
 */
abstract class TypeInfo(val variance: Variance) {
    object Empty: TypeInfo(Variance.INVARIANT) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> = Collections.emptyList()
    }

    class ByExpression(val expression: KtExpression, variance: Variance): TypeInfo(variance) {
        override fun getPossibleNamesFromExpression(bindingContext: BindingContext): Array<String> {
            return KotlinNameSuggester.suggestNamesByExpressionOnly(expression, bindingContext, { true }).toTypedArray()
        }

        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> =
                expression.guessTypes(
                        context = builder.currentFileContext,
                        module = builder.currentFileModule,
                        pseudocode = builder.pseudocode
                ).flatMap { it.getPossibleSupertypes(variance, builder) }
    }

    class ByTypeReference(val typeReference: KtTypeReference, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> =
                builder.currentFileContext[BindingContext.TYPE, typeReference].getPossibleSupertypes(variance, builder)
    }

    class ByType(val theType: KotlinType, variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> =
                theType.getPossibleSupertypes(variance, builder)
    }

    class ByReceiverType(variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> =
                (builder.placement as CallablePlacement.WithReceiver).receiverTypeCandidate.theType.getPossibleSupertypes(variance, builder)
    }

    abstract class DelegatingTypeInfo(val delegate: TypeInfo): TypeInfo(delegate.variance) {
        override val substitutionsAllowed: Boolean = delegate.substitutionsAllowed
        override fun getPossibleNamesFromExpression(bindingContext: BindingContext) = delegate.getPossibleNamesFromExpression(bindingContext)
        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> = delegate.getPossibleTypes(builder)
    }

    class NoSubstitutions(delegate: TypeInfo): DelegatingTypeInfo(delegate) {
        override val substitutionsAllowed: Boolean = false
    }

    class StaticContextRequired(delegate: TypeInfo): DelegatingTypeInfo(delegate) {
        override val staticContextRequired: Boolean = true
    }

    open val substitutionsAllowed: Boolean = true
    open val staticContextRequired: Boolean = false
    open fun getPossibleNamesFromExpression(bindingContext: BindingContext): Array<String> = ArrayUtil.EMPTY_STRING_ARRAY
    abstract fun getPossibleTypes(builder: CallableBuilder): List<KotlinType>

    private fun getScopeForTypeApproximation(config: CallableBuilderConfiguration, placement: CallablePlacement?): LexicalScope? {
        if (placement == null) return config.originalElement.getResolutionScope()

        val containingElement = when (placement) {
            is CallablePlacement.NoReceiver -> {
                placement.containingElement
            }
            is CallablePlacement.WithReceiver -> {
                val receiverClassDescriptor =
                        placement.receiverTypeCandidate.theType.constructor.declarationDescriptor
                val classDeclaration = receiverClassDescriptor?.let { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                if (!config.isExtension && classDeclaration != null) classDeclaration else config.currentFile
            }
        }
        return when (containingElement) {
            is KtClassOrObject -> (containingElement.resolveToDescriptor() as? ClassDescriptorWithResolutionScopes)?.scopeForMemberDeclarationResolution
            is KtBlockExpression -> (containingElement.statements.firstOrNull() ?: containingElement).getResolutionScope()
            is KtElement -> containingElement.containingKtFile.getResolutionScope()
            else -> null
        }
    }

    protected fun KotlinType?.getPossibleSupertypes(variance: Variance, callableBuilder: CallableBuilder): List<KotlinType> {
        if (this == null || ErrorUtils.containsErrorType(this)) {
            return Collections.singletonList(callableBuilder.currentFileModule.builtIns.anyType)
        }
        val scope = getScopeForTypeApproximation(callableBuilder.config, callableBuilder.placement)
        val approximations = getResolvableApproximations(scope, false, true)
        return when (variance) {
            Variance.IN_VARIANCE -> approximations.toList()
            else -> listOf(approximations.firstOrNull() ?: this)
        }
    }
}

fun TypeInfo(expressionOfType: KtExpression, variance: Variance): TypeInfo = TypeInfo.ByExpression(expressionOfType, variance)
fun TypeInfo(typeReference: KtTypeReference, variance: Variance): TypeInfo = TypeInfo.ByTypeReference(typeReference, variance)
fun TypeInfo(theType: KotlinType, variance: Variance): TypeInfo = TypeInfo.ByType(theType, variance)

fun TypeInfo.noSubstitutions(): TypeInfo = (this as? TypeInfo.NoSubstitutions) ?: TypeInfo.NoSubstitutions(this)

fun TypeInfo.forceNotNull(): TypeInfo {
    class ForcedNotNull(delegate: TypeInfo): TypeInfo.DelegatingTypeInfo(delegate) {
        override fun getPossibleTypes(builder: CallableBuilder): List<KotlinType> =
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
        val typeParameterInfos: List<TypeInfo>,
        val isAbstract: Boolean = false
) {
    abstract val kind: CallableKind
    abstract val parameterInfos: List<ParameterInfo>

    abstract fun copy(receiverTypeInfo: TypeInfo = this.receiverTypeInfo,
                      possibleContainers: List<KtElement> = this.possibleContainers,
                      isAbstract: Boolean = this.isAbstract): CallableInfo
}

class FunctionInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   possibleContainers: List<KtElement> = Collections.emptyList(),
                   override val parameterInfos: List<ParameterInfo> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList(),
                   val isOperator: Boolean = false,
                   val isInfix: Boolean = false,
                   isAbstract: Boolean = false
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos, isAbstract) {
    override val kind: CallableKind get() = CallableKind.FUNCTION

    override fun copy(receiverTypeInfo: TypeInfo, possibleContainers: List<KtElement>, isAbstract: Boolean) = FunctionInfo(
            name,
            receiverTypeInfo,
            returnTypeInfo,
            possibleContainers,
            parameterInfos,
            typeParameterInfos,
            isOperator,
            isInfix,
            isAbstract
    )
}

class PrimaryConstructorInfo(val classInfo: ClassInfo, expectedTypeInfo: TypeInfo): CallableInfo(
        classInfo.name, TypeInfo.Empty, expectedTypeInfo.forceNotNull(), Collections.emptyList(), classInfo.typeArguments, false
) {
    override val kind: CallableKind get() = CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR
    override val parameterInfos: List<ParameterInfo> get() = classInfo.parameterInfos

    override fun copy(receiverTypeInfo: TypeInfo, possibleContainers: List<KtElement>, isAbstract: Boolean) = throw UnsupportedOperationException()
}

class SecondaryConstructorInfo(
        override val parameterInfos: List<ParameterInfo>,
        val targetClass: PsiElement
): CallableInfo("", TypeInfo.Empty, TypeInfo.Empty, Collections.emptyList(), Collections.emptyList(), false) {
    override val kind: CallableKind get() = CallableKind.SECONDARY_CONSTRUCTOR

    override fun copy(receiverTypeInfo: TypeInfo, possibleContainers: List<KtElement>, isAbstract: Boolean) = throw UnsupportedOperationException()
}

class PropertyInfo(name: String,
                   receiverTypeInfo: TypeInfo,
                   returnTypeInfo: TypeInfo,
                   val writable: Boolean,
                   possibleContainers: List<KtElement> = Collections.emptyList(),
                   typeParameterInfos: List<TypeInfo> = Collections.emptyList(),
                   isAbstract: Boolean = false,
                   val isLateinitPreferred: Boolean = false
) : CallableInfo(name, receiverTypeInfo, returnTypeInfo, possibleContainers, typeParameterInfos, isAbstract) {
    override val kind: CallableKind get() = CallableKind.PROPERTY
    override val parameterInfos: List<ParameterInfo> get() = Collections.emptyList()

    override fun copy(receiverTypeInfo: TypeInfo, possibleContainers: List<KtElement>, isAbstract: Boolean) = PropertyInfo(
            name,
            receiverTypeInfo,
            returnTypeInfo,
            writable,
            possibleContainers,
            typeParameterInfos,
            isAbstract,
            isLateinitPreferred
    )
}
