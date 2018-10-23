/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

class ResultIsResultInspection : AbstractKotlinInspection() {

    private fun MemberScope.hasCorrespondingNonCatchingFunction(
        nameWithoutCatching: String,
        valueParameters: List<ValueParameterDescriptor>,
        returnType: KotlinType?,
        extensionReceiverType: KotlinType?
    ): Boolean {
        val nonCatchingFunctions = getContributedFunctions(Name.identifier(nameWithoutCatching), NoLookupLocation.FROM_IDE)
        return nonCatchingFunctions.any { nonCatchingFun ->
            nonCatchingFun.valueParameters.size == valueParameters.size
                    && nonCatchingFun.returnType == returnType
                    && nonCatchingFun.extensionReceiverParameter?.type == extensionReceiverType
        }
    }

    private fun FunctionDescriptor.hasCorrespondingNonCatchingFunction(returnType: KotlinType, nameWithoutCatching: String): Boolean? {
        val containingDescriptor = containingDeclaration
        val scope = when (containingDescriptor) {
            is ClassDescriptor -> containingDescriptor.unsubstitutedMemberScope
            is PackageFragmentDescriptor -> containingDescriptor.getMemberScope()
            else -> return null
        }
        val returnTypeArgument = returnType.arguments.firstOrNull()?.type
        val extensionReceiverType = extensionReceiverParameter?.type
        if (scope.hasCorrespondingNonCatchingFunction(nameWithoutCatching, valueParameters, returnTypeArgument, extensionReceiverType)) {
            return true
        }
        if (extensionReceiverType != null) {
            val extensionClassDescriptor = extensionReceiverType.constructor.declarationDescriptor as? ClassDescriptor
            if (extensionClassDescriptor != null) {
                val extensionClassScope = extensionClassDescriptor.unsubstitutedMemberScope
                if (extensionClassScope.hasCorrespondingNonCatchingFunction(
                        nameWithoutCatching, valueParameters, returnTypeArgument, extensionReceiverType = null
                    )
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun analyzeFunction(function: KtFunction, toReport: PsiElement, holder: ProblemsHolder) {
        if (function is KtConstructor<*>) return
        val returnTypeText = function.getReturnTypeReference()?.text
        if (returnTypeText != null && SHORT_NAME !in returnTypeText) return
        val name = (function as? KtNamedFunction)?.nameAsName?.asString()
        // Filter names from stdlib
        if (name in ALLOWED_NAMES) return
        if (function is KtNamedFunction) {
            val receiverTypeReference = function.receiverTypeReference
            // Filter Result extensions
            if (receiverTypeReference != null && SHORT_NAME in receiverTypeReference.text) return
        }
        if (function is KtFunctionLiteral || returnTypeText == null) {
            // Heuristics to save performance
            val text = function.bodyExpression?.text
            // Check there is something creating Result in function text
            if (text != null && ALLOWED_NAMES.none { it in text } && SHORT_NAME !in text && CATCHING !in text) return
        }

        val descriptor = function.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return
        val returnType = descriptor.returnType ?: return
        val returnTypeClass = returnType.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (returnTypeClass.fqNameSafe.asString() != FULL_NAME) return

        if (name != null && name.endsWith(CATCHING)) {
            val nameWithoutCatching = name.substringBeforeLast(CATCHING)
            if (descriptor.hasCorrespondingNonCatchingFunction(returnType, nameWithoutCatching) == false) {
                val returnTypeArgument = returnType.arguments.firstOrNull()?.type
                val typeName = returnTypeArgument?.constructor?.declarationDescriptor?.name?.asString() ?: "T"
                holder.registerProblem(
                    toReport,
                    "Function '$name' returning '$SHORT_NAME<$typeName>' without the corresponding " +
                            "function '$nameWithoutCatching' returning '$typeName'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
            }
        } else {
            holder.registerProblem(
                toReport,
                "Function returning $SHORT_NAME with a name that does not end with $CATCHING",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                *listOfNotNull(
                    AddGetOrThrowFix(withBody = function.hasBody()),
                    name?.let { RenameToCatchingFix("$it$CATCHING") }
                ).toTypedArray()
            )
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                analyzeFunction(function, function.nameIdentifier ?: function.funKeyword ?: function, holder)
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                analyzeFunction(lambdaExpression.functionLiteral, lambdaExpression.functionLiteral.lBrace, holder)
            }
        }
    }

    private class AddGetOrThrowFix(val withBody: Boolean) : LocalQuickFix {
        override fun getName(): String =
            if (withBody) "Add '.$GET_OR_THROW()' to function result (breaks use-sites!)"
            else "Unwrap '$SHORT_NAME' return type (breaks use-sites!)"

        override fun getFamilyName(): String = name

        private fun KtExpression.addGetOrThrow(factory: KtPsiFactory) {
            val getOrThrowExpression = factory.createExpressionByPattern("$0.$GET_OR_THROW()", this)
            replace(getOrThrowExpression)
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val function = descriptor.psiElement.getNonStrictParentOfType<KtFunction>() ?: return
            val returnTypeReference = function.getReturnTypeReference()
            val context = function.analyze()
            val functionDescriptor = context[BindingContext.FUNCTION, function] ?: return
            if (returnTypeReference != null) {
                val returnType = functionDescriptor.returnType ?: return
                val returnTypeArgument = returnType.arguments.firstOrNull()?.type ?: return
                function.setType(returnTypeArgument)
            }
            if (!withBody) return
            val factory = KtPsiFactory(project)
            val bodyExpression = function.bodyExpression
            bodyExpression?.forEachDescendantOfType<KtReturnExpression> {
                if (it.getTargetFunctionDescriptor(context) == functionDescriptor) {
                    it.returnedExpression?.addGetOrThrow(factory)
                }
            }
            if (function is KtFunctionLiteral) {
                val lastStatement = function.bodyExpression?.statements?.lastOrNull()
                if (lastStatement != null && lastStatement !is KtReturnExpression) {
                    lastStatement.addGetOrThrow(factory)
                }
            } else if (!function.hasBlockBody()) {
                bodyExpression?.addGetOrThrow(factory)
            }
        }
    }

    private class RenameToCatchingFix(val newName: String) : LocalQuickFix {
        override fun getName(): String = "Rename to '$newName'"

        override fun getFamilyName(): String = name

        override fun startInWriteAction(): Boolean = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val function = descriptor.psiElement.getNonStrictParentOfType<KtFunction>() ?: return
            RenameProcessor(project, function, newName, false, false).run()
        }
    }

    companion object {
        private const val SHORT_NAME = "Result"

        private const val FULL_NAME = "kotlin.$SHORT_NAME"

        private const val CATCHING = "Catching"

        private const val GET_OR_THROW = "getOrThrow"

        private val ALLOWED_NAMES = setOf("success", "failure", "runCatching")
    }
}