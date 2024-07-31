/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IrFunctionSignature.ParameterSignature
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KProperty

sealed class IrSignatureContainer(parent: IrSignatureContainer?) {
    val name: FqName = (parent?.name ?: FqName.ROOT).child(Name.identifier(this.javaClass.simpleName))
}

open class IrPackageSignature(parent: IrSignatureContainer?) : IrSignatureContainer(parent) {
    protected val Fun = IrFunctionSignature(
        callableId = null,
        extensionParameter = IrFunctionSignature.ParameterSignature.MISSING,
        dispatchParameter = IrFunctionSignature.ParameterSignature.MISSING,
        regularParameters = null,
    )
}

open class IrClassSignature(parent: IrSignatureContainer?) : IrSignatureContainer(parent), IrTypeSignature {
    val classId = ClassId(name.parent(), name.shortName()) // fixme

    override val classifier: ClassId?
        get() = classId
    override val isNullable: Boolean?
        get() = false

    protected val Fun = IrFunctionSignature(
        callableId = null,
        extensionParameter = ParameterSignature(IrTypeSignatureImpl(classId)),
        dispatchParameter = IrFunctionSignature.ParameterSignature.MISSING,
        regularParameters = null,
    )
}

data class IrFunctionSignature(
    val callableId: CallableId?,
    val dispatchParameter: ParameterSignature?,
    val extensionParameter: ParameterSignature?,
    val regularParameters: List<ParameterSignature>?,
) {
    operator fun invoke(vararg regularParameters: IrTypeSignature): IrFunctionSignature =
        copy(regularParameters = regularParameters.map { ParameterSignature(it) })

    fun extension(clazzType: IrClassSignature): IrFunctionSignature =
        copy(extensionParameter = ParameterSignature(IrTypeSignatureImpl(clazzType.classId)))

    operator fun provideDelegate(thisArg: Any?, property: KProperty<*>): IrFunctionSignature =
        copy(callableId = CallableId(Name.identifier(property.name)))

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisArg: Any?, property: KProperty<*>): IrFunctionSignature = this

    data class ParameterSignature(val type: IrTypeSignature) {
        companion object {
            val MISSING = ParameterSignature(IrTypeSignature.MISSING)
            val WILDCARD = ParameterSignature(IrTypeSignature.WILDCARD)
        }
    }
}

sealed interface IrTypeSignature {
    val classifier: ClassId?
    val isNullable: Boolean?

    companion object {
        val MISSING = IrTypeSignatureImpl(null, null)
        val WILDCARD = IrTypeSignatureImpl(null, null)

        inline fun <reified T> type(): IrTypeSignature = TODO()
    }
}

val IrTypeSignature.asNullable: IrTypeSignature
    get() = IrTypeSignatureImpl(classifier, true)

data class IrTypeSignatureImpl(
    override val classifier: ClassId?,
    override val isNullable: Boolean? = false,
) : IrTypeSignature


fun IrFunction.matches(signature: IrFunctionSignature): Boolean = TODO()