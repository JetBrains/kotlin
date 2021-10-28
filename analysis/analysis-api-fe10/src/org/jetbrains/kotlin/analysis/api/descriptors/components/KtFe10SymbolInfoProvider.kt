/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtSymbolInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

internal class KtFe10SymbolInfoProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtSymbolInfoProvider(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun getDeprecation(symbol: KtSymbol): DeprecationInfo? {
        val descriptor = getSymbolDescriptor(symbol) ?: return null
        ForceResolveUtil.forceResolveAllContents(descriptor)
        return analysisContext.deprecationResolver.getDeprecations(descriptor).firstOrNull()
    }

    override fun getDeprecation(symbol: KtSymbol, annotationUseSiteTarget: AnnotationUseSiteTarget?): DeprecationInfo? {
        return getDeprecation(symbol) // TODO
    }

    override fun getGetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        val getter = symbol.getter ?: return null
        val accessorDescriptor = getSymbolDescriptor(getter) as? PropertyAccessorDescriptor ?: return null
        ForceResolveUtil.forceResolveAllContents(accessorDescriptor.correspondingProperty)
        return analysisContext.deprecationResolver.getDeprecations(accessorDescriptor).firstOrNull()
    }

    override fun getSetterDeprecation(symbol: KtPropertySymbol): DeprecationInfo? {
        val getter = symbol.setter ?: return null
        val accessorDescriptor = getSymbolDescriptor(getter) as? PropertyAccessorDescriptor ?: return null
        ForceResolveUtil.forceResolveAllContents(accessorDescriptor.correspondingProperty)
        return analysisContext.deprecationResolver.getDeprecations(accessorDescriptor).firstOrNull()
    }

    override fun getJavaGetterName(symbol: KtPropertySymbol): Name {
        val descriptor = getSymbolDescriptor(symbol) as? PropertyDescriptor
        if (descriptor is SyntheticJavaPropertyDescriptor) {
            return descriptor.getMethod.name
        }

        if (descriptor != null) {
            val getter = descriptor.getter ?: return SpecialNames.NO_NAME_PROVIDED
            return Name.identifier(DescriptorUtils.getJvmName(getter) ?: JvmAbi.getterName(descriptor.name.asString()))
        }

        val ktPropertyName = (symbol.psi as? KtProperty)?.name ?: return SpecialNames.NO_NAME_PROVIDED
        return Name.identifier(JvmAbi.getterName(ktPropertyName))
    }

    override fun getJavaSetterName(symbol: KtPropertySymbol): Name? {
        val descriptor = getSymbolDescriptor(symbol) as? PropertyDescriptor
        if (descriptor is SyntheticJavaPropertyDescriptor) {
            return descriptor.setMethod?.name
        }

        if (descriptor != null) {
            if (!descriptor.isVar) {
                return null
            }

            val setter = descriptor.setter ?: return SpecialNames.NO_NAME_PROVIDED
            return Name.identifier(DescriptorUtils.getJvmName(setter) ?: JvmAbi.setterName(descriptor.name.asString()))
        }

        val ktPropertyName = (symbol.psi as? KtProperty)?.takeIf { it.isVar }?.name ?: return SpecialNames.NO_NAME_PROVIDED
        return Name.identifier(JvmAbi.setterName(ktPropertyName))
    }
}