/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator

class TypeTranslatorImpl(
    symbolTable: ReferenceSymbolTable,
    signatureComposer: IdSignatureComposer,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor,
    typeParametersResolverBuilder: () -> TypeParametersResolver = { ScopedTypeParametersResolver(signatureComposer) },
    enterTableScope: Boolean = false,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
) : TypeTranslator(symbolTable, languageVersionSettings, typeParametersResolverBuilder, enterTableScope, extensions) {
    override val constantValueGenerator: ConstantValueGenerator =
        ConstantValueGeneratorImpl(moduleDescriptor, symbolTable, this)

    private val typeApproximatorForNI = TypeApproximator(moduleDescriptor.builtIns, languageVersionSettings)

    override fun approximateType(type: KotlinType): KotlinType =
        typeApproximatorForNI.approximateDeclarationType(type, local = false)

    override fun commonSupertype(types: Collection<KotlinType>): KotlinType =
        CommonSupertypes.commonSupertype(types)

    init {
        signatureComposer.setupTypeApproximation { approximate(it) }
    }
}
