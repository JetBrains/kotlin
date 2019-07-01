/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.File

val IrConstructor.constructedClass get() = this.parent as IrClass

val <T : IrDeclaration> T.original get() = this

val IrDeclarationParent.fqNameForIrSerialization: FqName
    get() = when (this) {
        is IrPackageFragment -> this.fqName
        is IrDeclaration -> this.parent.fqNameForIrSerialization.child(this.nameForIrSerialization)
        else -> error(this)
    }

@Deprecated(
    "Use fqNameForIrSerialization instead.",
    ReplaceWith("fqNameForIrSerialization", "org.jetbrains.kotlin.ir.util.fqNameForIrSerialization"),
    DeprecationLevel.ERROR
)
val IrDeclarationParent.fqNameSafe: FqName
    get() = fqNameForIrSerialization

val IrClass.classId: ClassId?
    get() = when (val parent = this.parent) {
        is IrClass -> parent.classId?.createNestedClassId(this.name)
        is IrPackageFragment -> ClassId.topLevel(parent.fqName.child(this.name))
        else -> null
    }

val IrDeclaration.nameForIrSerialization: Name
    get() = when (this) {
        is IrDeclarationWithName -> this.name
        is IrConstructor -> SPECIAL_INIT_NAME
        else -> error(this)
    }
@Deprecated(
    "Use nameForIrSerialization instead.",
    ReplaceWith("nameForIrSerialization", "org.jetbrains.kotlin.ir.util.nameForIrSerialization"),
    DeprecationLevel.ERROR
)
val IrDeclaration.name: Name
    get() = nameForIrSerialization

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

private val IrConstructorCall.annotationClass
    get() = this.symbol.owner.constructedClass

fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
    any { it.annotationClass.fqNameWhenAvailable == fqName }

fun List<IrConstructorCall>.findAnnotation(fqName: FqName): IrConstructorCall? =
    firstOrNull { it.annotationClass.fqNameWhenAvailable == fqName }

val IrDeclaration.fileEntry: SourceManager.FileEntry
    get() = parent.let {
        when (it) {
            is IrFile -> it.fileEntry
            is IrPackageFragment -> TODO("Unknown file")
            is IrDeclaration -> it.fileEntry
            else -> TODO("Unexpected declaration parent")
        }
    }

fun IrClass.companionObject() = this.declarations.singleOrNull {it is IrClass && it.isCompanion }

val IrDeclaration.isGetter get() = this is IrSimpleFunction && this == this.correspondingPropertySymbol?.owner?.getter

val IrDeclaration.isSetter get() = this is IrSimpleFunction && this == this.correspondingPropertySymbol?.owner?.setter

val IrDeclaration.isAccessor get() = this.isGetter || this.isSetter

val IrDeclaration.isPropertyAccessor get() =
    this is IrSimpleFunction && this.correspondingPropertySymbol != null

val IrDeclaration.isPropertyField get() =
    this is IrField && this.correspondingPropertySymbol != null

val IrDeclaration.isTopLevelDeclaration get() =
    parent !is IrDeclaration && !this.isPropertyAccessor && !this.isPropertyField

fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when {
    this.isTopLevelDeclaration ->
        this
    this.isPropertyAccessor ->
        (this as IrSimpleFunction).correspondingPropertySymbol!!.owner.findTopLevelDeclaration()
    this.isPropertyField ->
        (this as IrField).correspondingPropertySymbol!!.owner.findTopLevelDeclaration()
    else ->
        (this.parent as IrDeclaration).findTopLevelDeclaration()
}

val IrDeclaration.isAnonymousObject get() = DescriptorUtils.isAnonymousObject(this.descriptor)
val IrDeclaration.isLocal get() = DescriptorUtils.isLocal(this.descriptor)

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

val SourceManager.FileEntry.lineStartOffsets
    get() = File(name).let {
        if (it.exists() && it.isFile) it.lineStartOffsets else IntArray(0)
    }

class NaiveSourceBasedFileEntryImpl(override val name: String, val lineStartOffsets: IntArray = IntArray(0)) : SourceManager.FileEntry {

    //-------------------------------------------------------------------------//

    override fun getLineNumber(offset: Int): Int {
        assert(offset != UNDEFINED_OFFSET)
        if (offset == SYNTHETIC_OFFSET) return 0
        val index = lineStartOffsets.binarySearch(offset)
        return if (index >= 0) index else -index - 2
    }

    //-------------------------------------------------------------------------//

    override fun getColumnNumber(offset: Int): Int {
        assert(offset != UNDEFINED_OFFSET)
        if (offset == SYNTHETIC_OFFSET) return 0
        val lineNumber = getLineNumber(offset)
        return offset - lineStartOffsets[lineNumber]
    }

    //-------------------------------------------------------------------------//

    override val maxOffset: Int
        //get() = TODO("not implemented")
        get() = UNDEFINED_OFFSET

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo {
        //TODO("not implemented")
        return SourceRangeInfo(name, beginOffset, -1, -1, endOffset, -1, -1)

    }
}
