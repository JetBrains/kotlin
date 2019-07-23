/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isLocalClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.org.objectweb.asm.Type

fun IrTypeMapper.mapType(irVariable: IrVariable): Type =
    mapType(irVariable.type)

fun IrTypeMapper.mapType(irValueParameter: IrValueParameter): Type =
    mapType(irValueParameter.type)

fun IrTypeMapper.mapType(irField: IrField): Type =
    mapType(irField.type)

fun IrTypeMapper.mapSupertype(irType: IrType, sw: JvmSignatureWriter): Type =
    mapType(irType, TypeMappingMode.SUPER_TYPE, sw)

fun IrTypeMapper.mapClass(irClass: IrClass): Type =
    mapType(irClass.defaultType, TypeMappingMode.CLASS_DECLARATION)

fun IrTypeMapper.mapTypeAsDeclaration(irType: IrType): Type =
    mapType(irType, TypeMappingMode.CLASS_DECLARATION)

fun IrTypeMapper.mapTypeParameter(irType: IrType, sw: JvmSignatureWriter): Type =
    mapType(irType, TypeMappingMode.GENERIC_ARGUMENT, sw)

internal fun getJvmShortName(klass: IrClass): String =
    klass.fqNameWhenAvailable?.toUnsafe()?.let { JavaToKotlinClassMap.mapKotlinToJava(it)?.shortClassName?.asString() }
        ?: SpecialNames.safeIdentifier(klass.name).identifier

internal class PossiblyInnerIrType(
    val classifier: IrClass,
    val arguments: List<IrTypeArgument>,
    private val outerType: PossiblyInnerIrType?
) {
    fun segments(): List<PossiblyInnerIrType> = outerType?.segments().orEmpty() + this
}

internal fun IrSimpleType.buildPossiblyInnerType(): PossiblyInnerIrType? =
    buildPossiblyInnerType(classOrNull?.owner, 0)

private fun IrSimpleType.buildPossiblyInnerType(classifier: IrClass?, index: Int): PossiblyInnerIrType? {
    if (classifier == null) return null

    val toIndex = classifier.typeParameters.size + index
    if (!classifier.isInner) {
        assert(toIndex == arguments.size || classifier.isLocalClass()) {
            "${arguments.size - toIndex} trailing arguments were found in $this type"
        }

        return PossiblyInnerIrType(classifier, arguments.subList(index, arguments.size), null)
    }

    val argumentsSubList = arguments.subList(index, toIndex)
    return PossiblyInnerIrType(
        classifier, argumentsSubList,
        buildPossiblyInnerType(classifier.parentAsClass, toIndex)
    )
}

internal val IrTypeParameter.representativeUpperBound: IrType
    get() {
        assert(superTypes.isNotEmpty()) { "Upper bounds should not be empty: $this" }

        return superTypes.firstOrNull {
            val irClass = it.classOrNull?.owner ?: return@firstOrNull false
            irClass.kind != ClassKind.INTERFACE && irClass.kind != ClassKind.ANNOTATION_CLASS
        } ?: superTypes.first()
    }
