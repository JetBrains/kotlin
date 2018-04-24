/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallWithIndexedArgumentsBase
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrTerminalDeclarationReferenceBase
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType

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
            val buffer = mutableListOf<Int>()
            var currentOffset = 0
            file.forEachLine { line ->
                buffer.add(currentOffset)
                currentOffset += line.length
            }
            lineStartOffsets = buffer.toIntArray()
        } else {
            lineStartOffsets = IntArray(0)
        }
    }

    //-------------------------------------------------------------------------//

    override fun getLineNumber(offset: Int): Int {
        val index = lineStartOffsets.binarySearch(offset)
        return if (index >= 0) index else -index - 1
    }

    //-------------------------------------------------------------------------//

    override fun getColumnNumber(offset: Int): Int {
        var lineNumber = getLineNumber(offset)
        if (lineNumber >= lineStartOffsets.size) {
            lineNumber = lineStartOffsets.size - 1
        }
        if (lineNumber < 0) lineNumber = 0
        if (lineStartOffsets.size == 0) return offset
        return offset - lineStartOffsets[lineNumber]
    }

    //-------------------------------------------------------------------------//

    override val maxOffset: Int
        get() = TODO("not implemented")

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo {
        TODO("not implemented")
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

interface IrSuspensionPoint : IrExpression {
    var suspensionPointIdParameter: IrVariable
    var result: IrExpression
    var resumeResult: IrExpression
}

interface IrSuspendableExpression : IrExpression {
    var suspensionPointId: IrExpression
    var result: IrExpression
}

class IrSuspensionPointImpl(startOffset: Int, endOffset: Int, type: KotlinType,
                            override var suspensionPointIdParameter: IrVariable,
                            override var result: IrExpression,
                            override var resumeResult: IrExpression)
    : IrExpressionBase(startOffset, endOffset, type), IrSuspensionPoint {

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointIdParameter.accept(visitor, data)
        result.accept(visitor, data)
        resumeResult.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        suspensionPointIdParameter = suspensionPointIdParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
        resumeResult = resumeResult.transform(transformer, data)
    }
}

class IrSuspendableExpressionImpl(startOffset: Int, endOffset: Int, type: KotlinType,
                                  override var suspensionPointId: IrExpression, override var result: IrExpression)
    : IrExpressionBase(startOffset, endOffset, type), IrSuspendableExpression {

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointId.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        suspensionPointId = suspensionPointId.transform(transformer, data)
        result = result.transform(transformer, data)
    }
}

internal interface IrPrivateFunctionCall : IrCall {
    val dfgSymbol: DataFlowIR.FunctionSymbol.Declared
    val moduleDescriptor: ModuleDescriptor
    val totalFunctions: Int
    val functionIndex: Int
}

internal class IrPrivateFunctionCallImpl(startOffset: Int,
                                         endOffset: Int,
                                         type: KotlinType,
                                         override val symbol: IrFunctionSymbol,
                                         override val descriptor: FunctionDescriptor,
                                         typeArgumentsCount: Int,
                                         override val dfgSymbol: DataFlowIR.FunctionSymbol.Declared,
                                         override val moduleDescriptor: ModuleDescriptor,
                                         override val totalFunctions: Int,
                                         override val functionIndex: Int
) : IrPrivateFunctionCall, IrCallWithIndexedArgumentsBase(
        startOffset,
        endOffset,
        type,
        typeArgumentsCount = typeArgumentsCount,
        valueArgumentsCount = symbol.descriptor.valueParameters.size
) {

    override val superQualifierSymbol: IrClassSymbol?
        get() = null

    override val superQualifier: ClassDescriptor?
        get() = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitCall(this, data)

}

internal interface IrPrivateClassReference : IrClassReference {
    val moduleDescriptor: ModuleDescriptor
    val totalClasses: Int
    val classIndex: Int
}

internal class IrPrivateClassReferenceImpl(startOffset: Int,
                                           endOffset: Int,
                                           type: KotlinType,
                                           symbol: IrClassifierSymbol,
                                           override val classType: KotlinType,
                                           override val moduleDescriptor: ModuleDescriptor,
                                           override val totalClasses: Int,
                                           override val classIndex: Int
) : IrPrivateClassReference,
        IrTerminalDeclarationReferenceBase<IrClassifierSymbol, ClassifierDescriptor>(
                startOffset, endOffset, type,
                symbol, symbol.descriptor
        )
{
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitClassReference(this, data)
}