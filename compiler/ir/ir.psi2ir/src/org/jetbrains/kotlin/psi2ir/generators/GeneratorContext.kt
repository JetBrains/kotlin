/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.SamTypeApproximator
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.generators.fragments.FragmentContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class GeneratorContext private constructor(
    val configuration: Psi2IrConfiguration,
    val moduleDescriptor: ModuleDescriptor,
    val bindingContext: BindingContext,
    val languageVersionSettings: LanguageVersionSettings,
    val symbolTable: SymbolTable,
    val extensions: GeneratorExtensions,
    val typeTranslator: TypeTranslator,
    override val irBuiltIns: IrBuiltIns,
    internal val callToSubstitutedDescriptorMap: MutableMap<IrDeclarationReference, CallableDescriptor>,
    internal val fragmentContext: FragmentContext?,
) : IrGeneratorContext {

    constructor(
        configuration: Psi2IrConfiguration,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext,
        languageVersionSettings: LanguageVersionSettings,
        symbolTable: SymbolTable,
        extensions: GeneratorExtensions,
        typeTranslator: TypeTranslator,
        irBuiltIns: IrBuiltIns,
        fragmentContext: FragmentContext? = null,
    ) : this(
        configuration,
        moduleDescriptor,
        bindingContext,
        languageVersionSettings,
        symbolTable,
        extensions,
        typeTranslator,
        irBuiltIns,
        mutableMapOf(),
        fragmentContext,
    )

    val constantValueGenerator = typeTranslator.constantValueGenerator

    fun IrDeclarationReference.commitSubstituted(descriptor: CallableDescriptor) {
        callToSubstitutedDescriptorMap[this] = descriptor
    }

    // TODO: inject a correct StorageManager instance, or store NotFoundClasses inside ModuleDescriptor
    val reflectionTypes = ReflectionTypes(moduleDescriptor, NotFoundClasses(LockBasedStorageManager.NO_LOCKS, moduleDescriptor))

    val additionalDescriptorStorage: DescriptorStorageForAdditionalReceivers = DescriptorStorageForAdditionalReceivers()

    val samTypeApproximator = SamTypeApproximator(moduleDescriptor.builtIns, languageVersionSettings)

    fun createFileScopeContext(ktFile: KtFile): GeneratorContext {
        return GeneratorContext(
            configuration,
            moduleDescriptor,
            bindingContext,
            languageVersionSettings,
            symbolTable,
            extensions,
            TypeTranslatorImpl(symbolTable, languageVersionSettings, moduleDescriptor, extensions = extensions, ktFile = ktFile),
            irBuiltIns,
            callToSubstitutedDescriptorMap,
            fragmentContext,
        )
    }
}
