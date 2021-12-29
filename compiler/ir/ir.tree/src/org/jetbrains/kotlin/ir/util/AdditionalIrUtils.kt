/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.io.File

val IrConstructor.constructedClass get() = this.parent as IrClass

val IrDeclarationParent.fqNameForIrSerialization: FqName
    get() = when (this) {
        is IrPackageFragment -> this.fqName
        is IrDeclaration -> this.parent.fqNameForIrSerialization.child(this.nameForIrSerialization)
        else -> error(this)
    }

/**
 * Skips synthetic FILE_CLASS to make top-level functions look as in kotlin source
 */
val IrDeclarationParent.kotlinFqName: FqName
    get() = when (this) {
        is IrPackageFragment -> this.fqName
        is IrClass -> {
            if (isFileClass) {
                parent.kotlinFqName
            } else {
                parent.kotlinFqName.child(nameForIrSerialization)
            }
        }
        is IrDeclaration -> this.parent.kotlinFqName.child(nameForIrSerialization)
        else -> error(this)
    }

val IrClass.classId: ClassId?
    get() = when (val parent = this.parent) {
        is IrClass -> parent.classId?.createNestedClassId(this.name)
        is IrPackageFragment -> ClassId.topLevel(parent.fqName.child(this.name))
        else -> null
    }

val IrDeclaration.nameForIrSerialization: Name
    get() = when (this) {
        is IrDeclarationWithName -> this.name
        is IrConstructor -> SpecialNames.INIT
        else -> error(this)
    }

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
    get() = symbol.signature?.packageFqName() ?: parent.getPackageFragment()?.fqName

fun IrDeclarationWithName.hasEqualFqName(fqName: FqName): Boolean =
    name == fqName.shortName() && when (val parent = parent) {
        is IrPackageFragment -> parent.fqName == fqName.parent()
        is IrDeclarationWithName -> parent.hasEqualFqName(fqName.parent())
        else -> false
    }

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

val IrDeclaration.isTopLevelDeclaration get() =
    parent !is IrDeclaration && !this.isPropertyAccessor && !this.isPropertyField

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

val File.lineStartOffsets: IntArray
    get() {
        // TODO: could be incorrect, if file is not in system's line terminator format.
        // Maybe use (0..document.lineCount - 1)
        //                .map { document.getLineStartOffset(it) }
        //                .toIntArray()
        // as in PSI.
        val separatorLength = System.lineSeparator().length
        val buffer = mutableListOf<Int>()
        var currentOffset = 0
        this.forEachLine { line ->
            buffer.add(currentOffset)
            currentOffset += line.length + separatorLength
        }
        buffer.add(currentOffset)
        return buffer.toIntArray()
    }

val IrFileEntry.lineStartOffsets: IntArray
    get() = when (this) {
        is PsiIrFileEntry -> this.getLineOffsets()
        else -> File(name).let { if (it.exists() && it.isFile) it.lineStartOffsets else IntArray(0) }
    }

class NaiveSourceBasedFileEntryImpl(
    override val name: String,
    private val lineStartOffsets: IntArray = intArrayOf()
) : IrFileEntry {
    val lineStartOffsetsAreEmpty: Boolean
        get() = lineStartOffsets.isEmpty()

    private val MAX_SAVED_LINE_NUMBERS = 50

    // Map with several last calculated line numbers.
    // Calculating for same offset is made many times during code and debug info generation.
    // In the worst case at least getting column recalculates line because it is usually called after getting line.
    private val calculatedBeforeLineNumbers = object : SLRUCache<Int, Int>(
        MAX_SAVED_LINE_NUMBERS / 2, MAX_SAVED_LINE_NUMBERS / 2
    ) {
        override fun createValue(key: Int): Int {
            val index = lineStartOffsets.binarySearch(key)
            return if (index >= 0) index else -index - 2
        }
    }

    override fun getLineNumber(offset: Int): Int {
        if (offset == SYNTHETIC_OFFSET) return 0
        if (offset < 0) return -1
        return calculatedBeforeLineNumbers.get(offset)
    }

    override fun getColumnNumber(offset: Int): Int {
        if (offset == SYNTHETIC_OFFSET) return 0
        if (offset < 0) return -1
        val lineNumber = getLineNumber(offset)
        return offset - lineStartOffsets[lineNumber]
    }

    override val maxOffset: Int
        get() = UNDEFINED_OFFSET

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo {
        return SourceRangeInfo(name, beginOffset, -1, -1, endOffset, -1, -1)
    }
}

private fun IrClass.getPropertyDeclaration(name: String): IrProperty? {
    val properties = declarations.filterIsInstance<IrProperty>().filter { it.name.asString() == name }
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

fun IrClass.getPropertyGetter(name: String): IrSimpleFunctionSymbol? =
    getPropertyDeclaration(name)?.getter?.symbol
        ?: getSimpleFunction("<get-$name>").also { assert(it?.owner?.correspondingPropertySymbol?.owner?.name?.asString() == name) }

fun IrClass.getPropertySetter(name: String): IrSimpleFunctionSymbol? =
    getPropertyDeclaration(name)?.setter?.symbol
        ?: getSimpleFunction("<set-$name>").also { assert(it?.owner?.correspondingPropertySymbol?.owner?.name?.asString() == name) }

fun IrClassSymbol.getSimpleFunction(name: String): IrSimpleFunctionSymbol? = owner.getSimpleFunction(name)
fun IrClassSymbol.getPropertyGetter(name: String): IrSimpleFunctionSymbol? = owner.getPropertyGetter(name)
fun IrClassSymbol.getPropertySetter(name: String): IrSimpleFunctionSymbol? = owner.getPropertySetter(name)

inline fun MemberScope.findFirstFunction(name: String, predicate: (CallableMemberDescriptor) -> Boolean) =
    getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).first(predicate)

fun filterOutAnnotations(fqName: FqName, annotations: List<IrConstructorCall>): List<IrConstructorCall> {
    return annotations.filterNot { it.annotationClass.hasEqualFqName(fqName) }
}
