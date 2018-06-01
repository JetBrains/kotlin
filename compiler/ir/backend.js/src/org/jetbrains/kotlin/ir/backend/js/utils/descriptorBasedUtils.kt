/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val IrConstructorSymbol.constructedClass get() = descriptor.constructedClass

val IrClassSymbol.isAny get() = KotlinBuiltIns.isAny(descriptor)

fun ModuleDescriptor.getFunctions(fqName: FqName): List<FunctionDescriptor> {
    return getFunctions(fqName.parent(), fqName.shortName())
}

fun ModuleDescriptor.getFunctions(packageFqName: FqName, name: Name): List<FunctionDescriptor> {
    return getPackage(packageFqName).memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()
}

val CallableMemberDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
        this.correspondingProperty
    else this