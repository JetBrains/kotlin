/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.irasdescriptors

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.descriptors.backingField
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

val IrConstructor.constructedClass get() = this.parent as IrClass

val <T : IrDeclaration> T.original get() = this
val IrDeclaration.containingDeclaration get() = this.parent

val IrDeclarationParent.fqNameSafe: FqName get() = when (this) {
    is IrPackageFragment -> this.fqName
    is IrDeclaration -> this.parent.fqNameSafe.child(this.name)

    else -> error(this)
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

val IrField.fqNameSafe: FqName get() = this.parent.fqNameSafe.child(this.name)

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
val IrFunction.allParameters: List<IrValueParameter>
    get() = if (this is IrConstructor) {
        listOf(this.constructedClass.thisReceiver
                ?: error(this.descriptor)
        ) + explicitParameters
    } else {
        explicitParameters
    }

val IrValueParameter.isVararg get() = this.varargElementType != null

val IrFunction.isSuspend get() = this is IrSimpleFunction && this.isSuspend

fun IrClass.isUnit() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.unit.toSafe()

fun IrClass.isKotlinArray() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.array.toSafe()

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.any.toSafe()
fun IrClass.isNothing() = this.fqNameSafe == KotlinBuiltIns.FQ_NAMES.nothing.toSafe()

fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

val IrProperty.konanBackingField: IrField?
    get() {
        assert(this.isReal)
        this.backingField?.let { return it }

        (this.descriptor as? DeserializedPropertyDescriptor)?.backingField?.let { backingFieldDescriptor ->
            val result = IrFieldImpl(
                    this.startOffset,
                    this.endOffset,
                    IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                    backingFieldDescriptor,
                    this.getter!!.returnType // TODO: this copies the behaviour found in backing field descriptor creation, but both are incorrect for property delegation.
            ).also {
                it.parent = this.parent
            }
            this.backingField = result
            return result
        }

        return null
    }

val IrField.containingClass get() = this.parent as? IrClass

val IrFunction.isReal get() = this.origin != IrDeclarationOrigin.FAKE_OVERRIDE

// Note: psi2ir doesn't set `origin = FAKE_OVERRIDE` for fields and properties yet.
val IrProperty.isReal: Boolean get() = this.descriptor.kind.isReal
val IrField.isReal: Boolean get() = this.descriptor.kind.isReal

val IrSimpleFunction.isOverridable: Boolean
    get() = visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && (parent as? IrClass)?.isFinalClass != true

val IrFunction.isOverridable get() = this is IrSimpleFunction && this.isOverridable

val IrFunction.isOverridableOrOverrides
    get() = this is IrSimpleFunction && (this.isOverridable || this.overriddenSymbols.isNotEmpty())

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS

fun IrSimpleFunction.overrides(other: IrSimpleFunction): Boolean {
    if (this == other) return true

    this.overriddenSymbols.forEach {
        if (it.owner.overrides(other)) {
            return true
        }
    }

    return false
}

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

internal val IrValueParameter.isValueParameter get() = this.index >= 0

private val IrCall.annotationClass
    get() = (this.symbol.owner as IrConstructor).constructedClass

fun List<IrCall>.hasAnnotation(fqName: FqName): Boolean =
        this.any { it.annotationClass.fqNameSafe == fqName }

fun IrAnnotationContainer.hasAnnotation(fqName: FqName) =
        this.annotations.hasAnnotation(fqName)

fun List<IrCall>.findAnnotation(fqName: FqName): IrCall? = this.firstOrNull {
    it.annotationClass.fqNameSafe == fqName
}
