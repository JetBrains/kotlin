/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.isJvm
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviatedType

class AddThrowsAnnotationIntention : SelfTargetingIntention<KtThrowExpression>(
    KtThrowExpression::class.java, "Add '@Throws' annotation"
) {

    override fun isApplicableTo(element: KtThrowExpression, caretOffset: Int): Boolean {
        if (!element.platform.isJvm()) return false
        val containingDeclaration = element.getContainingDeclaration() ?: return false

        val type = element.thrownExpression?.resolveToCall()?.resultingDescriptor?.returnType ?: return false
        if ((type.constructor.declarationDescriptor as? DeclarationDescriptorWithVisibility)?.visibility == Visibilities.LOCAL) return false

        val context = element.analyze(BodyResolveMode.PARTIAL)

        val annotationEntry = containingDeclaration.findThrowsAnnotation(context) ?: return true
        val valueArguments = annotationEntry.valueArguments
        if (valueArguments.isEmpty()) return true

        val argumentExpression = valueArguments.firstOrNull()?.getArgumentExpression()
        if (argumentExpression is KtCallExpression
            && argumentExpression.calleeExpression?.getCallableDescriptor()?.fqNameSafe != FqName("kotlin.arrayOf")
        ) return false

        return valueArguments.none { it.hasType(type, context) }
    }

    override fun applyTo(element: KtThrowExpression, editor: Editor?) {
        val containingDeclaration = element.getContainingDeclaration() ?: return
        val type = element.thrownExpression?.resolveToCall()?.resultingDescriptor?.returnType ?: return

        val annotationArgumentText = if (type.getAbbreviatedType() != null)
            "$type::class"
        else
            type.constructor.declarationDescriptor?.fqNameSafe?.let { "$it::class" } ?: return

        val context = element.analyze(BodyResolveMode.PARTIAL)
        val annotationEntry = containingDeclaration.findThrowsAnnotation(context)
        if (annotationEntry == null || annotationEntry.valueArguments.isEmpty()) {
            annotationEntry?.delete()
            val whiteSpaceText = if (containingDeclaration is KtPropertyAccessor) " " else "\n"
            containingDeclaration.addAnnotation(throwsAnnotationFqName, annotationArgumentText, whiteSpaceText)
        } else {
            val factory = KtPsiFactory(element)
            val argument = annotationEntry.valueArguments.firstOrNull()
            val expression = argument?.getArgumentExpression()
            val added = when {
                argument?.getArgumentName() == null ->
                    annotationEntry.valueArgumentList?.addArgument(factory.createArgument(annotationArgumentText))
                expression is KtCallExpression ->
                    expression.valueArgumentList?.addArgument(factory.createArgument(annotationArgumentText))
                expression is KtClassLiteralExpression -> {
                    expression.replaced(
                        factory.createCollectionLiteral(listOf(expression), annotationArgumentText)
                    ).getInnerExpressions().lastOrNull()
                }
                expression is KtCollectionLiteralExpression -> {
                    expression.replaced(
                        factory.createCollectionLiteral(expression.getInnerExpressions(), annotationArgumentText)
                    ).getInnerExpressions().lastOrNull()
                }
                else -> null
            }
            if (added != null) ShortenReferences.DEFAULT.process(added)
        }
    }
}

private val throwsAnnotationFqName = FqName("kotlin.jvm.Throws")

private fun KtThrowExpression.getContainingDeclaration(): KtDeclaration? {
    val parent = getParentOfTypesAndPredicate(
        true,
        KtNamedFunction::class.java,
        KtSecondaryConstructor::class.java,
        KtPropertyAccessor::class.java,
        KtClassInitializer::class.java,
        KtLambdaExpression::class.java
    ) { true }
    if (parent is KtClassInitializer || parent is KtLambdaExpression) return null
    return parent as? KtDeclaration
}

private fun KtDeclaration.findThrowsAnnotation(context: BindingContext): KtAnnotationEntry? {
    val annotationEntries = this.annotationEntries + (parent as? KtProperty)?.annotationEntries.orEmpty()
    return annotationEntries.find {
        val typeReference = it.typeReference ?: return@find false
        context[BindingContext.TYPE, typeReference]?.constructor?.declarationDescriptor?.fqNameSafe == throwsAnnotationFqName
    }
}

private fun ValueArgument.hasType(type: KotlinType, context: BindingContext): Boolean {
    val argumentExpression = getArgumentExpression()
    val expressions = when (argumentExpression) {
        is KtClassLiteralExpression -> listOf(argumentExpression)
        is KtCollectionLiteralExpression -> argumentExpression.getInnerExpressions().filterIsInstance(KtClassLiteralExpression::class.java)
        is KtCallExpression -> argumentExpression.valueArguments.mapNotNull { it.getArgumentExpression() as? KtClassLiteralExpression }
        else -> emptyList()
    }
    return expressions.any { it.getType(context)?.arguments?.firstOrNull()?.type == type }
}

private fun KtPsiFactory.createCollectionLiteral(expressions: List<KtExpression>, lastExpression: String): KtCollectionLiteralExpression {
    return buildExpression {
        appendFixedText("[")
        expressions.forEach {
            appendExpression(it)
            appendFixedText(", ")
        }
        appendFixedText(lastExpression)
        appendFixedText("]")
    } as KtCollectionLiteralExpression
}