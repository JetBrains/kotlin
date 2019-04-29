/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceManager.FileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.FqName

val File.lineStartOffsets: IntArray get() {
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

val FileEntry.lineStartOffsets get() = File(name).let {
    if (it.exists && it.isFile) it.lineStartOffsets else IntArray(0)
}


//-----------------------------------------------------------------------------//
/**
 * TODO
 * FileEntry provides mapping file offset to pair line and column which are used in debug information generation.
 * NaiveSourceBasedFileEntryImpl implements the functionality with calculation lines and columns at compile time,
 * that obligates user to have sources of all dependencies, that not always possible and intuitively clear. Instead new
 * version should rely on serialized mapping (offset to pair line and column), which should be generated at library
 * compilation stage (perhaps in or before inline face)
*/

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
        var lineNumber = getLineNumber(offset)
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

//-----------------------------------------------------------------------------//

class IrFileImpl(entry: SourceManager.FileEntry) : IrFile {

    override val fileEntry = entry

    //-------------------------------------------------------------------------//

    override val metadata: MetadataSource.File?
        get() = TODO("not implemented")
    override val annotations: MutableList<IrConstructorCall>
        get() = TODO("not implemented")
    override val fqName: FqName
        get() = TODO("not implemented")
    override val symbol: IrFileSymbol
        get() = TODO("not implemented")
    override val packageFragmentDescriptor: PackageFragmentDescriptor
        get() = TODO("not implemented")
    override val endOffset: Int
        get() = TODO("not implemented")
    override val startOffset: Int
        get() = TODO("not implemented")
    override val declarations: MutableList<IrDeclaration>
        get() = TODO("not implemented")
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        TODO("not implemented")
    }
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        TODO("not implemented")
    }
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        TODO("not implemented")
    }
}