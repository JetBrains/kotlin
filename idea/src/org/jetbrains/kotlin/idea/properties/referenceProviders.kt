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

package org.jetbrains.kotlin.idea.properties

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.properties.ResourceBundleReference
import com.intellij.lang.properties.references.PropertyReference
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.search.LocalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

private val PROPERTY_KEY = FqName(AnnotationUtil.PROPERTY_KEY)
private val PROPERTY_KEY_RESOURCE_BUNDLE = Name.identifier(AnnotationUtil.PROPERTY_KEY_RESOURCE_BUNDLE_PARAMETER)

private fun AnnotationDescriptor.getBundleName(): String? {
    return allValueArguments[PROPERTY_KEY_RESOURCE_BUNDLE]?.value as? String
}

private fun DeclarationDescriptor.getBundleNameByAnnotation(): String? {
    return (annotations.findAnnotation(PROPERTY_KEY) ?: annotations.findExternalAnnotation(PROPERTY_KEY))?.getBundleName()
}

private fun KtExpression.getBundleNameByContext(): String? {
    val expression = KtPsiUtil.safeDeparenthesize(this)
    val parent = expression.parent

    (parent as? KtProperty)?.let { return it.resolveToDescriptorIfAny()?.getBundleNameByAnnotation() }

    val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
    val resolvedCall =
            if (parent is KtQualifiedExpression && expression == parent.receiverExpression) {
                parent.selectorExpression.getResolvedCall(bindingContext)
            }
            else {
                expression.getParentResolvedCall(bindingContext)
            } ?: return null
    val callable = resolvedCall.resultingDescriptor

    if ((resolvedCall.extensionReceiver as? ExpressionReceiver)?.expression == expression) {
        val returnTypeAnnotations = callable.returnType?.annotations ?: return null
        return Annotations
                .findUseSiteTargetedAnnotation(returnTypeAnnotations, AnnotationUseSiteTarget.RECEIVER, PROPERTY_KEY)
                ?.getBundleName()
    }

    return resolvedCall.valueArguments.entries
            .singleOrNull { it.value.arguments.any { it.getArgumentExpression() == expression } }
            ?.key
            ?.getBundleNameByAnnotation()
}

private fun KtAnnotationEntry.getPropertyKeyResolvedCall(): ResolvedCall<*>? {
    val resolvedCall = getResolvedCall(analyze(BodyResolveMode.PARTIAL)) ?: return null
    val klass = (resolvedCall.resultingDescriptor as? ClassConstructorDescriptor)?.containingDeclaration ?: return null
    if (klass.kind != ClassKind.ANNOTATION_CLASS || klass.importableFqName != PROPERTY_KEY) return null
    return resolvedCall
}

private fun KtStringTemplateExpression.isBundleName(): Boolean {
    val parent = KtPsiUtil.safeDeparenthesize(this).parent
    when (parent) {
        is KtValueArgument -> {
            val resolvedCall = parent.getStrictParentOfType<KtAnnotationEntry>()?.getPropertyKeyResolvedCall() ?: return false
            val valueParameter = (resolvedCall.getArgumentMapping(parent) as? ArgumentMatch)?.valueParameter ?: return false
            if (valueParameter.name != PROPERTY_KEY_RESOURCE_BUNDLE) return false

            return true
        }

        is KtProperty -> {
            val contexts = (parent.useScope as? LocalSearchScope)?.scope ?: arrayOf(parent.containingFile)
            return contexts.any {
                it.anyDescendantOfType<KtAnnotationEntry> f@ { entry ->
                    if (!entry.valueArguments.any { it.getArgumentName()?.asName == PROPERTY_KEY_RESOURCE_BUNDLE }) return@f false
                    val resolvedCall = entry.getPropertyKeyResolvedCall() ?: return@f false
                    val parameter = resolvedCall.resultingDescriptor.valueParameters.singleOrNull { it.name == PROPERTY_KEY_RESOURCE_BUNDLE }
                                    ?: return@f false
                    val valueArgument = resolvedCall.valueArguments[parameter] as? ExpressionValueArgument ?: return@f false
                    val bundleNameExpression = valueArgument.valueArgument?.getArgumentExpression() ?: return@f false
                    bundleNameExpression is KtSimpleNameExpression && bundleNameExpression.mainReference.resolve() == parent
                }
            }
        }
    }

    return false
}

object KotlinPropertyKeyReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        if (!(element is KtStringTemplateExpression && element.isPlain())) return PsiReference.EMPTY_ARRAY
        val bundleName = element.getBundleNameByContext() ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(PropertyReference(ElementManipulators.getValueText(element), element, bundleName, false))
    }
}

object KotlinResourceBundleNameReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
        if (!(element is KtStringTemplateExpression && element.isPlain() && element.isBundleName())) return PsiReference.EMPTY_ARRAY
        return arrayOf(ResourceBundleReference(element))
    }
}