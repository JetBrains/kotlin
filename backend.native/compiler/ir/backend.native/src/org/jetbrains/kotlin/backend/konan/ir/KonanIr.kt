/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrTerminalDeclarationReferenceBase
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.FqName

//-----------------------------------------------------------------------------//
/**
 * TODO
 * FileEntry provides mapping file offset to pair line and column which are used in debug information generation.
 * NaiveSourceBasedFileEntryImpl implements the functionality with calculation lines and columns at compile time,
 * that obligates user to have sources of all dependencies, that not always possible and intuitively clear. Instead new
 * version should rely on serialized mapping (offset to pair line and column), which should be generated at library
 * compilation stage (perhaps in or before inline face)
*/

class NaiveSourceBasedFileEntryImpl(override val name: String) : SourceManager.FileEntry {

    private val lineStartOffsets: IntArray

    //-------------------------------------------------------------------------//

    init {
        val file = File(name)
        if (file.isFile) {
            // TODO: could be incorrect, if file is not in system's line terminator format.
            // Maybe use (0..document.lineCount - 1)
            //                .map { document.getLineStartOffset(it) }
            //                .toIntArray()
            // as in PSI.
            val separatorLength = System.lineSeparator().length
            val buffer = mutableListOf<Int>()
            var currentOffset = 0
            file.forEachLine { line ->
                buffer.add(currentOffset)
                currentOffset += line.length + separatorLength
            }
            buffer.add(currentOffset)
            lineStartOffsets = buffer.toIntArray()
        } else {
            lineStartOffsets = IntArray(0)
        }
    }

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

    override val annotations: MutableList<IrCall>
        get() = TODO("not implemented")
    override val fqName: FqName
        get() = TODO("not implemented")
    override val fileAnnotations: MutableList<AnnotationDescriptor>
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

//-----------------------------------------------------------------------------//

internal interface IrPrivateFunctionCall : IrExpression {
    val valueArgumentsCount: Int
    fun getValueArgument(index: Int): IrExpression?
    fun putValueArgument(index: Int, valueArgument: IrExpression?)
    fun removeValueArgument(index: Int)

    val virtualCallee: IrCall?
    val dfgSymbol: DataFlowIR.FunctionSymbol.Declared
    val moduleDescriptor: ModuleDescriptor
    val totalFunctions: Int
    val functionIndex: Int
}

internal class IrPrivateFunctionCallImpl(startOffset: Int,
                                         endOffset: Int,
                                         type: IrType,
                                         override val valueArgumentsCount: Int,
                                         override val virtualCallee: IrCall?,
                                         override val dfgSymbol: DataFlowIR.FunctionSymbol.Declared,
                                         override val moduleDescriptor: ModuleDescriptor,
                                         override val totalFunctions: Int,
                                         override val functionIndex: Int
) : IrPrivateFunctionCall, IrExpressionBase(startOffset, endOffset, type) {

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitExpression(this, data)
    }

    private val argumentsByParameterIndex: Array<IrExpression?> = arrayOfNulls(valueArgumentsCount)

    override fun getValueArgument(index: Int): IrExpression? {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        return argumentsByParameterIndex[index]
    }

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        argumentsByParameterIndex[index] = valueArgument
    }

    override fun removeValueArgument(index: Int) {
        argumentsByParameterIndex[index] = null
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argumentsByParameterIndex.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        argumentsByParameterIndex.forEachIndexed { i, irExpression ->
            argumentsByParameterIndex[i] = irExpression?.transform(transformer, data)
        }
    }
}

internal interface IrPrivateClassReference : IrClassReference {
    val moduleDescriptor: ModuleDescriptor
    val totalClasses: Int
    val classIndex: Int
    val dfgSymbol: DataFlowIR.Type.Declared
}

internal class IrPrivateClassReferenceImpl(startOffset: Int,
                                           endOffset: Int,
                                           type: IrType,
                                           symbol: IrClassifierSymbol,
                                           override val classType: IrType,
                                           override val moduleDescriptor: ModuleDescriptor,
                                           override val totalClasses: Int,
                                           override val classIndex: Int,
                                           override val dfgSymbol: DataFlowIR.Type.Declared
) : IrPrivateClassReference,
        IrTerminalDeclarationReferenceBase<IrClassifierSymbol, ClassifierDescriptor>(
                startOffset, endOffset, type,
                symbol, symbol.descriptor
        )
{
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitClassReference(this, data)
}