/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.regex.Pattern

private val functionPattern = Pattern.compile("^K?(Suspend)?Function\\d+$")

private val kotlinFqn = FqName("kotlin")

private val functionalPackages =
    listOf(kotlinFqn, kotlinFqn.child(Name.identifier("coroutines")), kotlinFqn.child(Name.identifier("reflect")))

fun isBuiltInFunction(value: IrDeclaration): Boolean = when (value) {
    is IrSimpleFunction ->
        value.name == OperatorNameConventions.INVOKE && (value.parent as? IrClass)?.let { isBuiltInFunction(it) } == true
    is IrClass ->
        value.fqNameWhenAvailable?.parent() in functionalPackages &&
                value.name.asString().let { functionPattern.matcher(it).find() }
    else -> false
}

fun isBuiltInFunction(value: DeclarationDescriptor): Boolean = when (value) {
    is FunctionInvokeDescriptor -> isBuiltInFunction(value.containingDeclaration)
    is ClassDescriptor -> {
        val fqn = (value.containingDeclaration as? PackageFragmentDescriptor)?.fqName
        functionalPackages.any { it == fqn } && value.name.asString().let { functionPattern.matcher(it).find() }
    }
    else -> false
}
