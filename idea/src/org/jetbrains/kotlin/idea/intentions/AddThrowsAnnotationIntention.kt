/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddThrowsAnnotationIntention : SelfTargetingIntention<KtThrowExpression>(
    KtThrowExpression::class.java, "Add @Throws annotation"
) {

    override fun isApplicableTo(element: KtThrowExpression, caretOffset: Int): Boolean {
        val thrownExpression = element.thrownExpression ?: return false
        thrownExpression.getThrowsAnnotationArgumentText() ?: return false
        val containingDeclaration = element.getContainingDeclaration() ?: return false

        val annotationEntry = containingDeclaration.findThrowsAnnotation() ?: return true
        val valueArguments = annotationEntry.valueArguments
        if (valueArguments.isEmpty()) return true

        val thrownClass = thrownExpression.getCallableDescriptor()?.containingDeclaration?.let {
            DescriptorToSourceUtils.descriptorToDeclaration(it)
        }
        return valueArguments.none {
            val expression = it.getArgumentExpression() as? KtClassLiteralExpression ?: return@none false
            val klass = expression.getChildOfType<KtReferenceExpression>()?.mainReference?.resolve() ?: return@none false
            klass == thrownClass
        }
    }

    override fun applyTo(element: KtThrowExpression, editor: Editor?) {
        val thrownExpression = element.thrownExpression ?: return
        val annotationArgumentText = thrownExpression.getThrowsAnnotationArgumentText() ?: return
        val containingDeclaration = element.getContainingDeclaration() ?: return

        val annotationEntry = containingDeclaration.findThrowsAnnotation()

        if (annotationEntry == null || annotationEntry.valueArguments.isEmpty()) {
            annotationEntry?.delete()
            containingDeclaration.addAnnotation(throwsAnnotationFqName, annotationArgumentText)
        } else {
            val added = annotationEntry.valueArgumentList?.addArgument(KtPsiFactory(element).createArgument(annotationArgumentText))
            added?.also { ShortenReferences.DEFAULT.process(it) }
        }
    }
}

private val throwsAnnotationFqName: FqName by lazy { FqName("kotlin.jvm.Throws") }

private fun KtExpression.getThrowsAnnotationArgumentText(): String? {
    val callee = getCalleeExpressionIfAny() ?: return null
    val receiverText = (this as? KtQualifiedExpression)?.receiverExpression?.let {
        "${it.text}."
    } ?: ""
    return "$receiverText${callee.text}::class"
}

private fun KtThrowExpression.getContainingDeclaration(): KtDeclaration? {
    return getParentOfTypesAndPredicate(
        true,
        KtNamedFunction::class.java,
        KtSecondaryConstructor::class.java,
        KtPropertyAccessor::class.java
    ) { true }
}

private fun KtDeclaration.findThrowsAnnotation(): KtAnnotationEntry? {
    val context = analyze(BodyResolveMode.PARTIAL)
    return annotationEntries.find {
        val typeReference = it.typeReference ?: return@find false
        context[BindingContext.TYPE, typeReference]?.constructor?.declarationDescriptor?.fqNameSafe == throwsAnnotationFqName
    }
}
