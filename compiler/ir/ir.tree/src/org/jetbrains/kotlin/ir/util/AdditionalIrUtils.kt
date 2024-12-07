/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

val IrConstructor.constructedClass get() = this.parent as IrClass

fun IrClassifierSymbol?.isArrayOrPrimitiveArray(builtins: IrBuiltIns): Boolean =
    this == builtins.arrayClass || this in builtins.primitiveArraysToPrimitiveTypes

// Constructors can't be marked as inline in metadata, hence this check.
fun IrFunction.isInlineArrayConstructor(): Boolean =
    this is IrConstructor && hasShape(regularParameters = 2) && constructedClass.defaultType.let { it.isArray() || it.isPrimitiveArray() }

val IrDeclarationParent.fqNameForIrSerialization: FqName
    get() = when (this) {
        is IrPackageFragment -> this.packageFqName
        is IrDeclarationWithName -> this.parent.fqNameForIrSerialization.child(this.name)
        else -> error(this)
    }

/**
 * Skips synthetic FILE_CLASS to make top-level functions look as in kotlin source
 */
val IrDeclarationParent.kotlinFqName: FqName
    get() = when (this) {
        is IrPackageFragment -> this.packageFqName
        is IrClass -> {
            if (isFileClass) {
                parent.kotlinFqName
            } else {
                parent.kotlinFqName.child(name)
            }
        }
        is IrDeclarationWithName -> this.parent.kotlinFqName.child(name)
        else -> error(this)
    }

val IrClass.classId: ClassId?
    get() = classIdImpl

val IrTypeAlias.classId: ClassId?
    get() = classIdImpl

private val IrDeclarationWithName.classIdImpl: ClassId?
    get() = when (val parent = this.parent) {
        is IrClass -> parent.classId?.createNestedClassId(this.name)
        is IrPackageFragment -> ClassId.topLevel(parent.packageFqName.child(this.name))
        is IrScript -> {
            // if the script is already lowered, use the target class as parent, otherwise use the package as parent, assuming that
            // the script to class lowering will rewrite it correctly
            parent.targetClass?.owner?.classId?.createNestedClassId(this.name)
                ?: (parent.parent as? IrFile)?.packageFqName?.child(this.name)?.let { ClassId.topLevel(it) }
        }
        else -> null
    }

val IrClass.classIdOrFail: ClassId
    get() = classIdOrFailImpl

val IrTypeAlias.classIdOrFail: ClassId
    get() = classIdOrFailImpl

private val IrDeclarationWithName.classIdOrFailImpl: ClassId
    get() = classIdImpl ?: error("No classId for $this")

val IrFunction.callableId: CallableId
    get() = callableIdImpl

val IrProperty.callableId: CallableId
    get() = callableIdImpl

val IrField.callableId: CallableId
    get() = callableIdImpl

val IrEnumEntry.callableId: CallableId
    get() = callableIdImpl

private val IrDeclarationWithName.callableIdImpl: CallableId
    get() {
        if (this.symbol is IrClassifierSymbol) error("Classifiers can not have callableId. Got $this")
        return when (val parent = this.parent) {
            is IrClass -> parent.classId?.let { CallableId(it, name) }
            is IrPackageFragment -> CallableId(parent.packageFqName, name)
            else -> null
        } ?: error("$this has no callableId")
    }

fun IrDeclaration.getNameWithAssert(): Name =
    if (this is IrDeclarationWithName) name else error(this)

val IrValueParameter.isVararg get() = this.varargElementType != null

val IrFunction.isSuspend get() = this is IrSimpleFunction && this.isSuspend

val IrFunction.isReal get() = !(this is IrSimpleFunction && isFakeOverride)

fun <S : IrSymbol> IrOverridableDeclaration<S>.overrides(other: IrOverridableDeclaration<S>): Boolean {
    if (this == other) return true

    this.overriddenSymbols.forEach {
        @Suppress("UNCHECKED_CAST")
        if ((it.owner as IrOverridableDeclaration<S>).overrides(other)) {
            return true
        }
    }

    return false
}

private val IrConstructorCall.annotationClass
    get() = this.symbol.owner.constructedClass

fun IrConstructorCall.isAnnotationWithEqualFqName(fqName: FqName): Boolean =
    annotationClass.hasEqualFqName(fqName)

val IrClass.packageFqName: FqName?
    get() = symbol.signature?.packageFqName() ?: parent.getPackageFragment()?.packageFqName

fun IrDeclarationWithName.hasEqualFqName(fqName: FqName): Boolean =
    name == fqName.shortName() && when (val parent = parent) {
        is IrPackageFragment -> parent.packageFqName == fqName.parent()
        is IrDeclarationWithName -> parent.hasEqualFqName(fqName.parent())
        else -> false
    }

fun IrDeclarationWithName.hasTopLevelEqualFqName(packageName: String, declarationName: String): Boolean =
    symbol.hasTopLevelEqualFqName(packageName, declarationName) || name.asString() == declarationName && when (val parent = parent) {
        is IrPackageFragment -> parent.packageFqName.asString() == packageName
        else -> false
    }

fun IrSymbol.hasEqualFqName(fqName: FqName): Boolean {
    return this is IrClassSymbol && with(signature as? IdSignature.CommonSignature ?: return false) {
        // optimized version of FqName("$packageFqName.$declarationFqName") == fqName
        val fqNameAsString = fqName.asString()
        fqNameAsString.length == packageFqName.length + 1 + declarationFqName.length &&
                fqNameAsString[packageFqName.length] == '.' &&
                fqNameAsString.startsWith(packageFqName) &&
                fqNameAsString.endsWith(declarationFqName)
    }
}

private fun IrSymbol.hasTopLevelEqualFqName(packageName: String, declarationName: String): Boolean {
    return this is IrClassSymbol && with(signature as? IdSignature.CommonSignature ?: return false) {
        // optimized version of FqName("$packageFqName.$declarationFqName") == fqName
        packageFqName == packageName && declarationFqName == declarationName
    }
}

fun List<IrConstructorCall>.hasAnnotation(classId: ClassId): Boolean = hasAnnotation(classId.asSingleFqName())

fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
    any { it.annotationClass.hasEqualFqName(fqName) }

fun List<IrConstructorCall>.findAnnotation(fqName: FqName): IrConstructorCall? =
    firstOrNull { it.annotationClass.hasEqualFqName(fqName) }

val IrDeclaration.fileEntry: IrFileEntry
    get() = parent.let {
        when (it) {
            is IrFile -> it.fileEntry
            is IrPackageFragment -> TODO("Unknown file")
            is IrDeclaration -> it.fileEntry
            else -> TODO("Unexpected declaration parent")
        }
    }

// This declaration accesses IrDeclarationContainer.declarations, which is marked with this opt-in
@UnsafeDuringIrConstructionAPI
fun IrClass.companionObject(): IrClass? =
    this.declarations.singleOrNull { it is IrClass && it.isCompanion } as IrClass?

val IrDeclaration.isGetter get() = this is IrSimpleFunction && this == this.correspondingPropertySymbol?.owner?.getter

val IrDeclaration.isSetter get() = this is IrSimpleFunction && this == this.correspondingPropertySymbol?.owner?.setter

val IrDeclaration.isAccessor get() = this.isGetter || this.isSetter

val IrDeclaration.isPropertyAccessor get() =
    this is IrSimpleFunction && this.correspondingPropertySymbol != null

val IrDeclaration.isPropertyField get() =
    this is IrField && this.correspondingPropertySymbol != null

val IrDeclaration.isJvmInlineClassConstructor get() =
    this is IrSimpleFunction && name.asString() == "constructor-impl"

val IrDeclaration.isAnonymousObject get() = this is IrClass && name == SpecialNames.NO_NAME_PROVIDED

val IrDeclaration.isAnonymousFunction get() = this is IrSimpleFunction && name == SpecialNames.NO_NAME_PROVIDED

val IrDeclaration.isLocal: Boolean
    get() {
        var current: IrElement = this
        while (current !is IrPackageFragment) {
            require(current is IrDeclaration)

            if (current is IrDeclarationWithVisibility) {
                if (current.visibility == DescriptorVisibilities.LOCAL) return true
            }

            if (current.isAnonymousObject) return true
            if (current is IrScript || (current is IrClass && current.origin == IrDeclarationOrigin.SCRIPT_CLASS)) return true

            current = current.parent
        }

        return false
    }

@ObsoleteDescriptorBasedAPI
val IrDeclaration.module get() = this.descriptor.module

const val SYNTHETIC_OFFSET = -2

class NaiveSourceBasedFileEntryImpl(
    override val name: String,
    override val lineStartOffsets: IntArray = intArrayOf(),
    override val maxOffset: Int = UNDEFINED_OFFSET
) : AbstractIrFileEntry() {
    val lineStartOffsetsAreEmpty: Boolean
        get() = lineStartOffsets.isEmpty()

    override fun getLineNumber(offset: Int): Int {
        if (offset == SYNTHETIC_OFFSET) return 0
        return super.getLineNumber(offset)
    }

    override fun getColumnNumber(offset: Int): Int {
        if (offset == SYNTHETIC_OFFSET) return 0
        return super.getColumnNumber(offset)
    }

    override fun getLineAndColumnNumbers(offset: Int): LineAndColumn {
        if (offset == SYNTHETIC_OFFSET) return LineAndColumn(0, 0)
        return super.getLineAndColumnNumbers(offset)
    }
}

// This declaration accesses IrDeclarationContainer.declarations, which is marked with this opt-in
@UnsafeDuringIrConstructionAPI
private fun IrClass.getPropertyDeclaration(name: String): IrProperty? {
    val properties = declarations.filterIsInstanceAnd<IrProperty> { it.name.asString() == name }
    if (properties.size > 1) {
        error(
            "More than one property with name $name in class $fqNameWhenAvailable:\n" +
                    properties.joinToString("\n", transform = IrProperty::render)
        )
    }
    return properties.firstOrNull()
}

fun IrClass.getSimpleFunction(name: String): IrSimpleFunctionSymbol? =
    findDeclaration<IrSimpleFunction> { it.name.asString() == name }?.symbol

// This declaration accesses IrDeclarationContainer.declarations, which is marked with this opt-in
@UnsafeDuringIrConstructionAPI
fun IrClass.getPropertyGetter(name: String): IrSimpleFunctionSymbol? =
    getPropertyDeclaration(name)?.getter?.symbol
        ?: getSimpleFunction("<get-$name>").also { assert(it?.owner?.correspondingPropertySymbol?.owner?.name?.asString() == name) }

// This declaration accesses IrDeclarationContainer.declarations, which is marked with this opt-in
@UnsafeDuringIrConstructionAPI
fun IrClass.getPropertySetter(name: String): IrSimpleFunctionSymbol? =
    getPropertyDeclaration(name)?.setter?.symbol
        ?: getSimpleFunction("<set-$name>").also { assert(it?.owner?.correspondingPropertySymbol?.owner?.name?.asString() == name) }

@UnsafeDuringIrConstructionAPI
fun IrClassSymbol.getSimpleFunction(name: String): IrSimpleFunctionSymbol? = owner.getSimpleFunction(name)

@UnsafeDuringIrConstructionAPI
fun IrClassSymbol.getPropertyGetter(name: String): IrSimpleFunctionSymbol? = owner.getPropertyGetter(name)

@UnsafeDuringIrConstructionAPI
fun IrClassSymbol.getPropertySetter(name: String): IrSimpleFunctionSymbol? = owner.getPropertySetter(name)

fun filterOutAnnotations(fqName: FqName, annotations: List<IrConstructorCall>): List<IrConstructorCall> {
    return annotations.filterNot { it.annotationClass.hasEqualFqName(fqName) }
}

fun IrFunction.isBuiltInSuspendCoroutine(): Boolean =
    isTopLevelInPackage("suspendCoroutine", StandardNames.COROUTINES_PACKAGE_FQ_NAME)

fun IrFunction.isBuiltInSuspendCoroutineUninterceptedOrReturn(): Boolean =
    isTopLevelInPackage(
        "suspendCoroutineUninterceptedOrReturn",
        StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME
    )
