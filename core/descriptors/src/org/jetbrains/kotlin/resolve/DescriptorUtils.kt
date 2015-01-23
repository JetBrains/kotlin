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

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.name.FqName

public fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor {
    val classObjectDescriptor = getClassObjectDescriptor()
    return if (classObjectDescriptor == null || hasSyntheticClassObject()) this else classObjectDescriptor
}

public fun ClassDescriptor.hasSyntheticClassObject(): Boolean = getKind() in setOf(ENUM_ENTRY, OBJECT)

public fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor =
        if (this is ConstructorDescriptor || DescriptorUtils.isClassObject(this)) getContainingDeclaration()!! else this

public val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && getExtensionReceiverParameter() != null

public val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

public fun ModuleDescriptor.resolveTopLevelClass(topLevelClassFqName: FqName): ClassDescriptor? {
    assert(!topLevelClassFqName.isRoot())
    return getPackage(topLevelClassFqName.parent())?.getMemberScope()
            ?.getClassifier(topLevelClassFqName.shortName()) as? ClassDescriptor
}