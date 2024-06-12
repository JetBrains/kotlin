/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil

internal class KaFe10SymbolInformationProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaSessionComponent<KaFe10Session>(), KaSymbolInformationProvider, KaFe10SessionComponent {
    override val KaSymbol.deprecationStatus: DeprecationInfo?
        get() = withValidityAssertion {
            val descriptor = getSymbolDescriptor(this) ?: return null
            ForceResolveUtil.forceResolveAllContents(descriptor)
            return getDeprecation(descriptor)
        }

    override fun KaSymbol.deprecationStatus(annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? = withValidityAssertion {
        when (annotationUseSiteTarget) {
            AnnotationUseSiteTarget.PROPERTY_GETTER -> {
                if (this is KaPropertySymbol) {
                    return (getter ?: this).deprecationStatus
                }
            }
            AnnotationUseSiteTarget.PROPERTY_SETTER -> {
                if (this is KaPropertySymbol) {
                    return (setter ?: this).deprecationStatus
                }
            }
            AnnotationUseSiteTarget.SETTER_PARAMETER -> {
                if (this is KaPropertySymbol) {
                    return (setter?.parameter ?: this).deprecationStatus
                }
            }
            else -> {}
        }
        return deprecationStatus // TODO
    }

    private fun getDeprecation(descriptor: DeclarationDescriptor): DeprecationInfo? {
        if (descriptor is PropertyDescriptor) {
            val fieldDescriptor = descriptor.backingField
            if (fieldDescriptor != null && fieldDescriptor.annotations.hasAnnotation(DeprecationResolver.JAVA_DEPRECATED)) {
                return SimpleDeprecationInfo(DeprecationLevelValue.WARNING, propagatesToOverrides = false, message = null)
            }
        }

        return analysisContext.deprecationResolver.getDeprecations(descriptor).firstOrNull()
    }

    private fun getAccessorDeprecation(
        property: KaPropertySymbol,
        accessor: KaPropertyAccessorSymbol?,
        accessorDescriptorProvider: (PropertyDescriptor) -> PropertyAccessorDescriptor?
    ): DeprecationInfo? {
        val propertyDescriptor = getSymbolDescriptor(property) as? PropertyDescriptor ?: return null
        ForceResolveUtil.forceResolveAllContents(propertyDescriptor)

        if (accessor != null) {
            val accessorDescriptor = getSymbolDescriptor(accessor) as? PropertyAccessorDescriptor
            if (accessorDescriptor != null) {
                ForceResolveUtil.forceResolveAllContents(accessorDescriptor.correspondingProperty)
                val deprecation = analysisContext.deprecationResolver.getDeprecations(accessorDescriptor).firstOrNull()
                if (deprecation != null) {
                    return deprecation
                }
            }
        }

        val accessorDescriptor = accessorDescriptorProvider(propertyDescriptor)
        if (accessorDescriptor != null) {
            val deprecation = analysisContext.deprecationResolver.getDeprecations(accessorDescriptor).firstOrNull()
            if (deprecation != null) {
                return deprecation
            }
        }

        return getDeprecation(propertyDescriptor)
    }

    override val KaPropertySymbol.getterDeprecationStatus: DeprecationInfo?
        get() = withValidityAssertion { getAccessorDeprecation(this, getter) { it.getter } }

    override val KaPropertySymbol.setterDeprecationStatus: DeprecationInfo?
        get() = withValidityAssertion { getAccessorDeprecation(this, setter) { it.setter } }

    override val KaClassOrObjectSymbol.annotationApplicableTargets: Set<KotlinTarget>?
        get() = withValidityAssertion {
            val descriptor = getSymbolDescriptor(this) as? ClassDescriptor ?: return null
            if (descriptor.kind != ClassKind.ANNOTATION_CLASS) return null

            return AnnotationChecker.applicableTargetSet(descriptor)
        }
}