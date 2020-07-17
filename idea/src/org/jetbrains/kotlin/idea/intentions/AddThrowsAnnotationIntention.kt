/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.JVM_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviatedType

class AddThrowsAnnotationIntention : SelfTargetingIntention<KtThrowExpression>(
    KtThrowExpression::class.java, KotlinBundle.lazyMessage("add.throws.annotation")
) {
    override fun isApplicableTo(element: KtThrowExpression, caretOffset: Int): Boolean {
        if (element.platform.isJs()) return false
        val containingDeclaration = element.getContainingDeclaration() ?: return false

        val type = element.thrownExpression?.resolveToCall()?.resultingDescriptor?.returnType ?: return false
        if ((type.constructor.declarationDescriptor as? DeclarationDescriptorWithVisibility)?.visibility == Visibilities.LOCAL) return false

        val module = element.module ?: return false
        if (!KOTLIN_THROWS_ANNOTATION_FQ_NAME.fqNameIsExists(module) &&
            !(element.platform.isJvm() && JVM_THROWS_ANNOTATION_FQ_NAME.fqNameIsExists(module))
        ) return false

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
            val annotationFqName = KOTLIN_THROWS_ANNOTATION_FQ_NAME.takeIf {
                element.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4
            } ?: JVM_THROWS_ANNOTATION_FQ_NAME

            containingDeclaration.addAnnotation(annotationFqName, annotationArgumentText, whiteSpaceText)
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
        val fqName = context[BindingContext.TYPE, typeReference]?.fqName ?: return@find false
        fqName == KOTLIN_THROWS_ANNOTATION_FQ_NAME || fqName == JVM_THROWS_ANNOTATION_FQ_NAME
    }
}

private fun ValueArgument.hasType(type: KotlinType, context: BindingContext): Boolean =
    when (val argumentExpression = getArgumentExpression()) {
        is KtClassLiteralExpression -> listOf(argumentExpression)
        is KtCollectionLiteralExpression -> argumentExpression.getInnerExpressions().filterIsInstance(KtClassLiteralExpression::class.java)
        is KtCallExpression -> argumentExpression.valueArguments.mapNotNull { it.getArgumentExpression() as? KtClassLiteralExpression }
        else -> emptyList()
    }.any { it.getType(context)?.arguments?.firstOrNull()?.type == type }

private fun KtPsiFactory.createCollectionLiteral(expressions: List<KtExpression>, lastExpression: String): KtCollectionLiteralExpression =
    buildExpression {
        appendFixedText("[")
        expressions.forEach {
            appendExpression(it)
            appendFixedText(", ")
        }
        appendFixedText(lastExpression)
        appendFixedText("]")
    } as KtCollectionLiteralExpression

private fun FqName.fqNameIsExists(module: Module): Boolean = KotlinFullClassNameIndex.getInstance()[
        asString(),
        module.project,
        GlobalSearchScope.moduleWithLibrariesScope(module)
].isNotEmpty()