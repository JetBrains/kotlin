/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
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
            SourceElement.NO_SOURCE, false, storageManager
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
