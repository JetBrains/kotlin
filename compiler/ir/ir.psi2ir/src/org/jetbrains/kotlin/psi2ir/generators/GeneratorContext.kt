/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class GeneratorContext(
    val configuration: Psi2IrConfiguration,
    val moduleDescriptor: ModuleDescriptor,
    val bindingContext: BindingContext,
    val languageVersionSettings: LanguageVersionSettings,
    val symbolTable: SymbolTable,
    val extensions: GeneratorExtensions,
    val typeTranslator: TypeTranslator,
    val constantValueGenerator: ConstantValueGenerator,
    override val irBuiltIns: IrBuiltIns
) : IrGeneratorContext {
    val callToSubstitutedDescriptorMap = mutableMapOf<IrDeclarationReference, CallableDescriptor>()

    val sourceManager = PsiSourceManager()

    // TODO: inject a correct StorageManager instance, or store NotFoundClasses inside ModuleDescriptor
    val reflectionTypes = ReflectionTypes(moduleDescriptor, NotFoundClasses(LockBasedStorageManager.NO_LOCKS, moduleDescriptor))

    fun IrDeclarationReference.commitSubstituted(descriptor: CallableDescriptor) {
        callToSubstitutedDescriptorMap[this] = descriptor
    }
}
