/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*

open class TypeTranslatorImpl(
    symbolTable: ReferenceSymbolTable,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor,
    typeParametersResolverBuilder: () -> TypeParametersResolver = { ScopedTypeParametersResolver() },
    enterTableScope: Boolean = false,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
    private val ktFile: KtFile? = null
) : TypeTranslator(symbolTable, languageVersionSettings, typeParametersResolverBuilder, enterTableScope, extensions) {
    override val constantValueGenerator: ConstantValueGenerator = ConstantValueGeneratorImpl(moduleDescriptor, symbolTable, this)

    private val typeApproximatorForNI = TypeApproximator(moduleDescriptor.builtIns, languageVersionSettings)

    private val typeApproximatorConfiguration =
        object : TypeApproximatorConfiguration.AllFlexibleSameValue() {
            override val allFlexible: Boolean get() = true
            override val errorType: Boolean get() = true
            override val integerLiteralConstantType: Boolean get() = true
            override val intersectionTypesInContravariantPositions: Boolean get() = true
        }

    override fun approximateType(type: KotlinType): KotlinType =
        substituteAlternativesInPublicType(type).let {
            typeApproximatorForNI.approximateToSuperType(it, typeApproximatorConfiguration) ?: it
        }

    override fun commonSupertype(types: Collection<KotlinType>): KotlinType =
        CommonSupertypes.commonSupertype(types)

    override fun isTypeAliasAccessibleHere(typeAliasDescriptor: TypeAliasDescriptor): Boolean {
        if (!DescriptorVisibilities.isPrivate(typeAliasDescriptor.visibility)) return true

        val psiFile = typeAliasDescriptor.source.getPsi()?.containingFile ?: return false

        return psiFile == ktFile
    }
}
