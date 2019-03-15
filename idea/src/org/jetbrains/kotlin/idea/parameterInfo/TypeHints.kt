/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.renderer.renderFqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isEnum

//hack to separate type presentation from param info presentation
const val TYPE_INFO_PREFIX = "@TYPE@"

class ImportAwareClassifierNamePolicy(
    val bindingContext: BindingContext,
    val context: KtElement
) : ClassifierNamePolicy {
    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
        if (classifier.containingDeclaration is ClassDescriptor) {
            val resolutionFacade = context.getResolutionFacade()
            val scope = context.getResolutionScope(bindingContext, resolutionFacade)
            if (scope.findClassifier(classifier.name, NoLookupLocation.FROM_IDE) == classifier) {
                return classifier.name.asString()
            }
        }

        return shortNameWithCompanionNameSkip(classifier, renderer)
    }

    private fun shortNameWithCompanionNameSkip(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
        if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

        val qualifiedNameParts = classifier.parentsWithSelf
            .takeWhile { it is ClassifierDescriptor }
            .filter { !(it.isCompanionObject() && it.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) }
            .mapTo(ArrayList()) { it.name }
            .reversed()

        return renderFqName(qualifiedNameParts)
    }
}

fun getInlayHintsTypeRenderer(bindingContext: BindingContext, context: KtElement) =
    DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
        enhancedTypes = true
        textFormat = RenderingFormat.PLAIN
        renderUnabbreviatedType = false
        classifierNamePolicy = ImportAwareClassifierNamePolicy(bindingContext, context)
    }

fun providePropertyTypeHint(elem: PsiElement): List<InlayInfo> {
    (elem as? KtCallableDeclaration)?.let { property ->
        property.nameIdentifier?.let { ident ->
            return provideTypeHint(property, ident.endOffset)
        }
    }
    return emptyList()
}

fun provideTypeHint(element: KtCallableDeclaration, offset: Int): List<InlayInfo> {
    var type: KotlinType = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element).unwrap()
    if (type.containsError()) return emptyList()
    val name = type.constructor.declarationDescriptor?.name
    if (name == SpecialNames.NO_NAME_PROVIDED) {
        if (element is KtProperty && element.isLocal) {
            // for local variables, an anonymous object type is not collapsed to its supertype,
            // so showing the supertype will be misleading
            return emptyList()
        }
        type = type.immediateSupertypes().singleOrNull() ?: return emptyList()
    } else if (name?.isSpecial == true) {
        return emptyList()
    }

    return if (isUnclearType(type, element)) {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
            .getCustomSettings(KotlinCodeStyleSettings::class.java)

        val declString = buildString {
            append(TYPE_INFO_PREFIX)
            if (settings.SPACE_BEFORE_TYPE_COLON) {
                append(" ")
            }
            append(":")
            if (settings.SPACE_AFTER_TYPE_COLON) {
                append(" ")
            }
            append(getInlayHintsTypeRenderer(element.analyze(), element).renderType(type))
        }
        listOf(InlayInfo(declString, offset, false, true, true))
    } else {
        emptyList()
    }
}

private fun isUnclearType(type: KotlinType, element: KtCallableDeclaration): Boolean {
    if (element !is KtProperty) return true

    val initializer = element.initializer ?: return true
    if (initializer is KtConstantExpression || initializer is KtStringTemplateExpression) return false
    if (initializer is KtUnaryExpression && initializer.baseExpression is KtConstantExpression) return false

    if (isConstructorCall(initializer)) {
        return false
    }

    if (initializer is KtDotQualifiedExpression) {
        val selectorExpression = initializer.selectorExpression
        if (type.isEnum()) {
            // Do not show type for enums if initializer has enum entry with explicit enum name: val p = Enum.ENTRY
            val enumEntryDescriptor: DeclarationDescriptor? = selectorExpression?.resolveMainReferenceToDescriptors()?.singleOrNull()

            if (enumEntryDescriptor != null && DescriptorUtils.isEnumEntry(enumEntryDescriptor)) {
                return false
            }
        }

        if (initializer.receiverExpression.isClassOrPackageReference() && isConstructorCall(selectorExpression)) {
            return false
        }
    }

    return true
}

private fun isConstructorCall(initializer: KtExpression?): Boolean {
    if (initializer is KtCallExpression) {
        val resolvedCall = initializer.resolveToCall(BodyResolveMode.FULL)
        val resolvedDescriptor = resolvedCall?.candidateDescriptor
        if (resolvedDescriptor is SamConstructorDescriptor) {
            return true
        }
        if (resolvedDescriptor is ConstructorDescriptor &&
            (resolvedDescriptor.constructedClass.declaredTypeParameters.isEmpty() || initializer.typeArgumentList != null)
        ) {
            return true
        }
        return false
    }

    return false
}

private fun KtExpression.isClassOrPackageReference(): Boolean =
    when (this) {
        is KtNameReferenceExpression -> this.resolveMainReferenceToDescriptors().singleOrNull().let { it is ClassDescriptor || it is PackageViewDescriptor }
        is KtDotQualifiedExpression -> this.selectorExpression?.isClassOrPackageReference() ?: false
        else -> false
    }
