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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

/**
 * Produces descriptors representing the fictitious classes for function types, such as kotlin.Function1 or kotlin.reflect.KFunction2.
 */
class BuiltInFictitiousFunctionClassFactory(
        private val storageManager: StorageManager,
        private val module: ModuleDescriptor
) : ClassDescriptorFactory {
    @OptIn(AllowedToUsedOnlyInK1::class)
    override fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean {
        val string = name.asString()
        return (string.startsWith("Function") || string.startsWith("KFunction") ||
                string.startsWith("SuspendFunction") || string.startsWith("KSuspendFunction")) // an optimization
               && FunctionTypeKindExtractor.Default.getFunctionalClassKindWithArity(packageFqName, string) != null
    }

    @OptIn(AllowedToUsedOnlyInK1::class)
    override fun createClass(classId: ClassId): ClassDescriptor? {
        if (classId.isLocal || classId.isNestedClass) return null

        val className = classId.relativeClassName.asString()
        if ("Function" !in className) return null // An optimization

        val packageFqName = classId.packageFqName
        val (kind, arity) = FunctionTypeKindExtractor.Default.getFunctionalClassKindWithArity(packageFqName, className) ?: return null


        val builtInsFragments = module.getPackage(packageFqName).fragments.filterIsInstance<BuiltInsPackageFragment>()

        // JS IR backend uses separate FunctionInterfacePackageFragment for function interfaces
        val containingPackageFragment =
            builtInsFragments.filterIsInstance<FunctionInterfacePackageFragment>().firstOrNull() ?: builtInsFragments.first()

        return FunctionClassDescriptor(storageManager, containingPackageFragment, kind, arity)
    }

    override fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor> {
        // We don't want to return 256 classes here since it would cause them to appear in every import list of every file
        // and likely slow down compilation very much
        return emptySet()
    }
}
