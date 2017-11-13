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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

class JvmBuiltInClassDescriptorFactory(
        storageManager: StorageManager,
        private val moduleDescriptor: ModuleDescriptor,
        private val computeContainingDeclaration: (ModuleDescriptor) -> DeclarationDescriptor = { module ->
            module.getPackage(KOTLIN_FQ_NAME).fragments.filterIsInstance<BuiltInsPackageFragment>().first()
        }
) : ClassDescriptorFactory {
    private val cloneable by storageManager.createLazyValue {
        ClassDescriptorImpl(
                computeContainingDeclaration(moduleDescriptor),
                CLONEABLE_NAME, Modality.ABSTRACT, ClassKind.INTERFACE, listOf(moduleDescriptor.builtIns.anyType),
                SourceElement.NO_SOURCE, /* isExternal = */ false
        ).apply {
            initialize(CloneableClassScope(storageManager, this), emptySet(), null)
        }
    }

    override fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean =
            name == CLONEABLE_NAME && packageFqName == KOTLIN_FQ_NAME

    override fun createClass(classId: ClassId): ClassDescriptor? =
            when (classId) {
                CLONEABLE_CLASS_ID -> cloneable
                else -> null
            }

    override fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor> =
            when (packageFqName) {
                KOTLIN_FQ_NAME -> setOf(cloneable)
                else -> emptySet()
            }

    companion object {
        private val KOTLIN_FQ_NAME = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
        private val CLONEABLE_NAME = KotlinBuiltIns.FQ_NAMES.cloneable.shortName()
        val CLONEABLE_CLASS_ID = ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.cloneable.toSafe())
    }
}
