/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val IrConstructor.constructedClass get() = this.parent as IrClass

val <T : IrDeclaration> T.original get() = this

val IrDeclarationParent.fqNameSafe: FqName
    get() = when (this) {
        is IrPackageFragment -> this.fqName
        is IrDeclaration -> this.parent.fqNameSafe.child(this.name)

        else -> error(this)
    }

val IrClass.classId: ClassId?
    get() {
        val parent = this.parent
        return when (parent) {
            is IrClass -> parent.classId?.createNestedClassId(this.name)
            is IrPackageFragment -> ClassId.topLevel(parent.fqName.child(this.name))
            else -> null
        }
    }

val IrDeclaration.name: Name
    get() = when (this) {
        is IrSimpleFunction -> this.name
        is IrClass -> this.name
        is IrEnumEntry -> this.name
        is IrProperty -> this.name
        is IrLocalDelegatedProperty -> this.name
        is IrField -> this.name
        is IrVariable -> this.name
        is IrConstructor -> SPECIAL_INIT_NAME
        is IrValueParameter -> this.name
        else -> error(this)
    }

private val SPECIAL_INIT_NAME = Name.special("<init>")

val IrValueParameter.isVararg get() = this.varargElementType != null

val IrFunction.isSuspend get() = this is IrSimpleFunction && this.isSuspend

val IrFunction.isReal get() = this.origin != IrDeclarationOrigin.FAKE_OVERRIDE

fun IrSimpleFunction.overrides(other: IrSimpleFunction): Boolean {
    if (this == other) return true

    this.overriddenSymbols.forEach {
        if (it.owner.overrides(other)) {
            return true
        }
    }

    return false
}

private val IrCall.annotationClass
    get() = (this.symbol.owner as IrConstructor).constructedClass

fun List<IrCall>.hasAnnotation(fqName: FqName): Boolean =
    this.any { it.annotationClass.fqNameSafe == fqName }

fun IrAnnotationContainer.hasAnnotation(fqName: FqName) =
    this.annotations.hasAnnotation(fqName)

fun List<IrCall>.findAnnotation(fqName: FqName): IrCall? = this.firstOrNull {
    it.annotationClass.fqNameSafe == fqName
}

fun IrClass.companionObject() = this.declarations.singleOrNull {it is IrClass && it.isCompanion }

val IrDeclaration.isGetter get() = this is IrSimpleFunction && this == this.correspondingProperty?.getter

val IrDeclaration.isSetter get() = this is IrSimpleFunction && this == this.correspondingProperty?.setter

val IrDeclaration.isAccessor get() = this.isGetter || this.isSetter

val IrDeclaration.fileEntry: SourceManager.FileEntry
    get() = parent.let {
        when (it) {
            is IrFile -> it.fileEntry
            is IrPackageFragment -> TODO("Unknown file")
            is IrDeclaration -> it.fileEntry
            else -> TODO("Unexpected declaration parent")
        }
    }