/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizer
import org.jetbrains.kotlin.analysis.api.components.KtImportOptimizerResult
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor

internal class KtFe10ImportOptimizer(
    override val analysisSession: KtFe10AnalysisSession,
) : KtImportOptimizer(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun analyseImports(file: KtFile): KtImportOptimizerResult = withValidityAssertion {
        error("FE10 implementation of KtImportOptimizer should not be called from anywhere")
    }

    override fun getImportableName(symbol: KtSymbol): FqName? {
        require(symbol is KtFe10Symbol)

        val descriptor = getSymbolDescriptor(symbol)
        if (descriptor?.canBeReferencedViaImport() != true) return null

        return descriptor.getImportableDescriptor().fqNameSafe
    }

    /**
     * Copy of `org.jetbrains.kotlin.idea.imports.ImportsUtils.canBeReferencedViaImport`.
     */
    private fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
        if (this is PackageViewDescriptor ||
            DescriptorUtils.isTopLevelDeclaration(this) ||
            this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this)
        ) {
            return !name.isSpecial
        }

        //Both TypeAliasDescriptor and ClassDescriptor
        val parentClassifier = containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return false
        if (!parentClassifier.canBeReferencedViaImport()) return false

        return when (this) {
            is ConstructorDescriptor -> !parentClassifier.isInner // inner class constructors can't be referenced via import
            is ClassDescriptor, is TypeAliasDescriptor -> true
            else -> parentClassifier is ClassDescriptor && parentClassifier.kind == ClassKind.OBJECT
        }
    }
}