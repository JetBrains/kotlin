/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.idea.util.getDataFlowAwareTypes
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.util.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

sealed class CreateCallableFromCallActionFactory<E : KtExpression>(
    extensionsEnabled: Boolean = true
) : CreateCallableMemberFromUsageFactory<E>(extensionsEnabled) {
    protected abstract fun doCreateCallableInfo(
        expression: E,
        analysisResult: AnalysisResult,
        name: String,
        receiverType: TypeInfo,
        possibleContainers: List<KtElement>
    ): CallableInfo?

    protected fun getExpressionOfInterest(diagnostic: Diagnostic): KtExpression? {
        val diagElement = diagnostic.psiElement
        if (PsiTreeUtil.getParentOfType(
                diagElement,
                KtTypeReference::class.java, KtAnnotationEntry::class.java, KtImportDirective::class.java
            ) != null
        ) return null

        return when (diagnostic.factory) {
            in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS, Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND -> {
                val parent = diagElement.parent
                if (parent is KtCallExpression && parent.calleeExpression == diagElement) parent else diagElement
            }

            Errors.NO_VALUE_FOR_PARAMETER,
            Errors.TOO_MANY_ARGUMENTS,
            Errors.NONE_APPLICABLE -> diagElement.getNonStrictParentOfType<KtCallExpression>()

            Errors.TYPE_MISMATCH -> (diagElement.parent as? KtValueArgument)?.getStrictParentOfType<KtCallExpression>()

            else -> throw AssertionError("Unexpected diagnostic: ${diagnostic.factory}")
        } as? KtExpression
    }

    override fun createCallableInfo(element: E, diagnostic: Diagnostic): CallableInfo? {
        val project = element.project

        val calleeExpr = when (element) {
            is KtCallExpression -> element.calleeExpression
            is KtSimpleNameExpression -> element
            else -> null
        } as? KtSimpleNameExpression ?: return null

        if (calleeExpr.getReferencedNameElementType() != KtTokens.IDENTIFIER) return null

        val analysisResult = calleeExpr.analyzeAndGetResult()
        val receiver = element.getCall(analysisResult.bindingContext)?.explicitReceiver
        val receiverType = getReceiverTypeInfo(analysisResult.bindingContext, project, receiver) ?: return null

        val possibleContainers =
            if (receiverType is TypeInfo.Empty) {
                val containers = with(element.getQualifiedExpressionForSelectorOrThis().getExtractionContainers()) {
                    if (element is KtCallExpression) this else filter { it is KtClassBody || it is KtFile }
                }
                if (containers.isNotEmpty()) containers else return null
            } else listOf()

        return doCreateCallableInfo(element, analysisResult, calleeExpr.getReferencedName(), receiverType, possibleContainers)
    }

    private fun getReceiverTypeInfo(context: BindingContext, project: Project, receiver: Receiver?): TypeInfo? {
        return when (receiver) {
            null -> TypeInfo.Empty
            is Qualifier -> {
                val qualifierType = context.getType(receiver.expression)
                if (qualifierType != null) return TypeInfo(qualifierType, Variance.IN_VARIANCE)

                if (receiver !is ClassQualifier) return null
                val classifierType = receiver.descriptor.classValueType
                if (classifierType != null) return TypeInfo(classifierType, Variance.IN_VARIANCE)

                val javaClassifier = receiver.descriptor as? JavaClassDescriptor ?: return null
                val javaClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, javaClassifier) as? PsiClass
                if (javaClass == null || !javaClass.canRefactor()) return null
                TypeInfo.StaticContextRequired(TypeInfo(javaClassifier.defaultType, Variance.IN_VARIANCE))
            }
            is ReceiverValue -> {
                val originalType = receiver.type
                val finalType = if (receiver is ExpressionReceiver) {
                    getDataFlowAwareTypes(
                        receiver.expression,
                        context,
                        originalType
                    ).firstOrNull() ?: originalType
                } else originalType
                TypeInfo(finalType, Variance.IN_VARIANCE)
            }
            else -> throw AssertionError("Unexpected receiver: $receiver")
        }
    }

    protected fun getAbstractCallableInfo(mainCallable: CallableInfo, originalExpression: KtExpression): CallableInfo? {
        val receiverTypeInfo: TypeInfo
        val receiverType: KotlinType

        val originalReceiverTypeInfo = mainCallable.receiverTypeInfo
        if (originalReceiverTypeInfo != TypeInfo.Empty) {
            if (originalReceiverTypeInfo !is TypeInfo.ByType) return null
            receiverTypeInfo = originalReceiverTypeInfo
            receiverType = receiverTypeInfo.theType
        } else {
            val containingClass = originalExpression.getStrictParentOfType<KtClassOrObject>() as? KtClass ?: return null
            if (containingClass is KtEnumEntry || containingClass.isAnnotation()) return null
            receiverType = (containingClass.unsafeResolveToDescriptor() as ClassDescriptor).defaultType
            receiverTypeInfo = TypeInfo(receiverType, Variance.IN_VARIANCE).ofThis()
        }

        if (!receiverType.isAbstract() && TypeUtils.getAllSupertypes(receiverType).all { !it.isAbstract() }) return null

        return mainCallable.copy(
            receiverTypeInfo = receiverTypeInfo,
            possibleContainers = emptyList(),
            modifierList = KtPsiFactory(originalExpression).createModifierList(KtTokens.ABSTRACT_KEYWORD)
        )
    }

    protected fun getCallableWithReceiverInsideExtension(
        mainCallable: CallableInfo,
        originalExpression: KtExpression,
        context: BindingContext,
        receiverType: TypeInfo
    ): CallableInfo? {
        if (receiverType != TypeInfo.Empty) return null
        val callable = (originalExpression.getParentOfTypeAndBranch<KtFunction> { bodyExpression }
            ?: originalExpression.getParentOfTypeAndBranches<KtProperty> { listOf(getter, setter) })
            ?: return null
        if (callable !is KtFunctionLiteral && callable.receiverTypeReference == null) return null
        val callableDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, callable] as? CallableDescriptor ?: return null
        val extensionReceiverType = callableDescriptor.extensionReceiverParameter?.type ?: return null
        val newReceiverTypeInfo = TypeInfo(extensionReceiverType, Variance.IN_VARIANCE)
        return mainCallable.copy(receiverTypeInfo = newReceiverTypeInfo, possibleContainers = emptyList())
    }

    sealed class Property : CreateCallableFromCallActionFactory<KtSimpleNameExpression>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): KtSimpleNameExpression? {
            val refExpr = getExpressionOfInterest(diagnostic) as? KtNameReferenceExpression ?: return null
            if (refExpr.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } != null) return null
            return refExpr
        }

        override fun doCreateCallableInfo(
            expression: KtSimpleNameExpression,
            analysisResult: AnalysisResult,
            name: String,
            receiverType: TypeInfo,
            possibleContainers: List<KtElement>
        ): CallableInfo? {
            val fullCallExpr = expression.getQualifiedExpressionForSelectorOrThis()
            val varExpected = fullCallExpr.getAssignmentByLHS() != null
            val expressionForTypeGuess = fullCallExpr.getExpressionForTypeGuess()
            val returnTypes = expressionForTypeGuess.guessTypes(analysisResult.bindingContext, analysisResult.moduleDescriptor)
            val returnTypeInfo = TypeInfo(expressionForTypeGuess, if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE)
            val canBeLateinit =
                varExpected
                        && returnTypes.any { !it.isMarkedNullable && !KotlinBuiltIns.isPrimitiveType(it) }
                        && fullCallExpr.parents
                    .firstOrNull { it is KtDeclarationWithBody || it is KtClassInitializer } is KtDeclarationWithBody
            return PropertyInfo(name, receiverType, returnTypeInfo, varExpected, possibleContainers, isLateinitPreferred = canBeLateinit)
        }

        object Default : Property() {
            override fun doCreateCallableInfo(
                expression: KtSimpleNameExpression,
                analysisResult: AnalysisResult,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<KtElement>
            ): CallableInfo? {
                return super.doCreateCallableInfo(
                    expression,
                    analysisResult,
                    name,
                    receiverType,
                    possibleContainers.filterNot { it is KtClassBody && (it.parent as KtClassOrObject).isInterfaceClass() }
                )
            }
        }

        object Abstract : Property() {
            override fun doCreateCallableInfo(
                expression: KtSimpleNameExpression,
                analysisResult: AnalysisResult,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<KtElement>
            ) = super.doCreateCallableInfo(expression, analysisResult, name, receiverType, possibleContainers)?.let {
                getAbstractCallableInfo(it, expression)
            }
        }

        object ByImplicitExtensionReceiver : Property() {
            override fun doCreateCallableInfo(
                expression: KtSimpleNameExpression,
                analysisResult: AnalysisResult,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<KtElement>
            ) = super.doCreateCallableInfo(expression, analysisResult, name, receiverType, possibleContainers)?.let {
                getCallableWithReceiverInsideExtension(
                    it,
                    expression,
                    analysisResult.bindingContext,
                    receiverType
                )
            }
        }
    }

    sealed class Function : CreateCallableFromCallActionFactory<KtCallExpression>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
            return getExpressionOfInterest(diagnostic) as? KtCallExpression
        }

        override fun doCreateCallableInfo(
            expression: KtCallExpression,
            analysisResult: AnalysisResult,
            name: String,
            receiverType: TypeInfo,
            possibleContainers: List<KtElement>
        ): CallableInfo? {
            val parameters = expression.getParameterInfos()
            val typeParameters = expression.getTypeInfoForTypeArguments()
            val fullCallExpression = expression.getQualifiedExpressionForSelectorOrThis()
            val expectedType = fullCallExpression.guessTypes(analysisResult.bindingContext, analysisResult.moduleDescriptor).singleOrNull()
            val returnType = if (expectedType != null) {
                TypeInfo(expectedType, Variance.OUT_VARIANCE)
            } else {
                TypeInfo(fullCallExpression, Variance.OUT_VARIANCE)
            }
            val parentFunction = expression.getStrictParentOfType<KtNamedFunction>()
            val modifierList = if (parentFunction?.hasModifier(KtTokens.INLINE_KEYWORD) == true) {
                when {
                    parentFunction.isPublic -> KtPsiFactory(expression).createModifierList(KtTokens.PUBLIC_KEYWORD)
                    parentFunction.isProtected() -> KtPsiFactory(expression).createModifierList(KtTokens.PROTECTED_KEYWORD)
                    else -> null
                }
            } else {
                null
            }
            return FunctionInfo(name, receiverType, returnType, possibleContainers, parameters, typeParameters, modifierList = modifierList)
        }

        object Default : Function()

        object Abstract : Function() {
            override fun doCreateCallableInfo(
                expression: KtCallExpression,
                analysisResult: AnalysisResult,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<KtElement>
            ) = super.doCreateCallableInfo(expression, analysisResult, name, receiverType, possibleContainers)?.let {
                getAbstractCallableInfo(it, expression)
            }
        }

        object ByImplicitExtensionReceiver : Function() {
            override fun doCreateCallableInfo(
                expression: KtCallExpression,
                analysisResult: AnalysisResult,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<KtElement>
            ) = super.doCreateCallableInfo(expression, analysisResult, name, receiverType, possibleContainers)?.let {
                getCallableWithReceiverInsideExtension(it, expression, analysisResult.bindingContext, receiverType)
            }
        }
    }

    object Constructor : CreateCallableFromCallActionFactory<KtCallExpression>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
            return getExpressionOfInterest(diagnostic) as? KtCallExpression
        }

        override fun doCreateCallableInfo(
            expression: KtCallExpression,
            analysisResult: AnalysisResult,
            name: String,
            receiverType: TypeInfo,
            possibleContainers: List<KtElement>
        ): CallableInfo? {
            if (expression.typeArguments.isNotEmpty()) return null

            val classDescriptor = expression
                .calleeExpression
                ?.getReferenceTargets(analysisResult.bindingContext)
                ?.mapNotNull { (it as? ConstructorDescriptor)?.containingDeclaration }
                ?.distinct()
                ?.singleOrNull() as? ClassDescriptor
            val klass = classDescriptor?.source?.getPsi()
            if ((klass !is KtClass && klass !is PsiClass) || !klass.canRefactor()) return null

            val expectedType =
                analysisResult.bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expression.getQualifiedExpressionForSelectorOrThis()]
                    ?: classDescriptor.builtIns.nullableAnyType
            if (!classDescriptor.defaultType.isSubtypeOf(expectedType)) return null

            val parameters = expression.getParameterInfos()

            return ConstructorInfo(parameters, klass)
        }
    }

    companion object {
        val FUNCTIONS = arrayOf(
            Function.Default,
            Function.Abstract,
            Function.ByImplicitExtensionReceiver,
            Constructor
        )
        val INSTANCES = arrayOf(
            Function.Default,
            Function.Abstract,
            Function.ByImplicitExtensionReceiver,
            Constructor,
            Property.Default,
            Property.Abstract,
            Property.ByImplicitExtensionReceiver
        )
    }
}
