/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

val IrConstructorSymbol.constructedClass get() = descriptor.constructedClass

val IrClassSymbol.isAny get() = KotlinBuiltIns.isAny(descriptor)

fun ModuleDescriptor.getFunctions(fqName: FqName): List<FunctionDescriptor> {
    return getFunctions(fqName.parent(), fqName.shortName())
}

fun ModuleDescriptor.getFunctions(packageFqName: FqName, name: Name): List<FunctionDescriptor> {
    return getPackage(packageFqName).memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()
}

fun ModuleDescriptor.getClassifier(fqName: FqName): ClassifierDescriptor? {
    return getClassifier(fqName.parent(), fqName.shortName())
}

fun ModuleDescriptor.getClassifier(packageFqName: FqName, name: Name): ClassifierDescriptor? {
    return getPackage(packageFqName).memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND)
}

fun createValueParameter(containingDeclaration: CallableDescriptor, index: Int, name: String, type: KotlinType): ValueParameterDescriptor {
    return ValueParameterDescriptorImpl(
        containingDeclaration = containingDeclaration,
        original = null,
        index = index,
        annotations = Annotations.EMPTY,
        name = Name.identifier(name),
        outType = type,
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = SourceElement.NO_SOURCE
    )
}