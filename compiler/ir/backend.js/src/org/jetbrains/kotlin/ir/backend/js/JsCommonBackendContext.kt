/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.InlineClassesUtils
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.backend.js.utils.isDispatchReceiver
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.isOverridableOrOverrides
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

interface JsCommonBackendContext : CommonBackendContext {
    val internalPackageFqn: FqName

    val reflectionSymbols: ReflectionSymbols
    val propertyLazyInitialization: PropertyLazyInitialization

    override val inlineClassesUtils: JsCommonInlineClassesUtils

    override val symbols: JsCommonSymbols
    val symbolTable: SymbolTable

    val jsPromiseSymbol: IrClassSymbol?

    val catchAllThrowableType: IrType
        get() = irBuiltIns.throwableType

    val es6mode: Boolean
        get() = false

    val externalPackageFragment: MutableMap<IrFileSymbol, IrFile>
    val additionalExportedDeclarations: Set<IrDeclaration>
    val bodilessBuiltInsPackageFragment: IrPackageFragment
}

fun findClass(memberScope: MemberScope, name: Name): ClassDescriptor =
    memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

fun findFunctions(memberScope: MemberScope, name: Name): List<SimpleFunctionDescriptor> =
    memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()

interface JsCommonInlineClassesUtils : InlineClassesUtils {

    /**
     * Returns the inlined class for the given type, or `null` if the type is not inlined.
     */
    fun getInlinedClass(type: IrType): IrClass?

    fun isTypeInlined(type: IrType): Boolean {
        return getInlinedClass(type) != null
    }

    fun shouldValueParameterBeBoxed(parameter: IrValueParameter): Boolean {
        val function = parameter.parent as? IrSimpleFunction ?: return false
        val klass = function.parent as? IrClass ?: return false
        if (!isClassInlineLike(klass)) return false
        return parameter.isDispatchReceiver && function.isOverridableOrOverrides
    }

    /**
     * An intrinsic for creating an instance of an inline class from its underlying value.
     */
    val boxIntrinsic: IrSimpleFunctionSymbol

    /**
     * An intrinsic for obtaining the underlying value from an instance of an inline class.
     */
    val unboxIntrinsic: IrSimpleFunctionSymbol
}
