/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.serialization.deserialization.NotFoundClasses
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class GeneratorContext(
        val configuration: Psi2IrConfiguration,
        val moduleDescriptor: ModuleDescriptor,
        val bindingContext: BindingContext
) : IrGeneratorContext(IrBuiltIns(moduleDescriptor.builtIns)) {
    val sourceManager = PsiSourceManager()

    val symbolTable = SymbolTable()

    // TODO: inject a correct StorageManager instance, or store NotFoundClasses inside ModuleDescriptor
    val reflectionTypes = ReflectionTypes(moduleDescriptor, NotFoundClasses(LockBasedStorageManager.NO_LOCKS, moduleDescriptor))
}
