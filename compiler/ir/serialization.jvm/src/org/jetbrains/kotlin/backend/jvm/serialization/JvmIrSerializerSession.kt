/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.synthetic.isVisibleOutside

class JvmIrSerializerSession(
    messageLogger: IrMessageLogger,
    private val declarationTable: DeclarationTable,
    private val mode: JvmSerializeIrMode,
    private val fileClassFqName: FqName,
    languageVersionSettings: LanguageVersionSettings,
) : IrFileSerializer(
    messageLogger, declarationTable, compatibilityMode = CompatibilityMode.CURRENT,
    languageVersionSettings = languageVersionSettings,
    bodiesOnlyForInlines = mode == JvmSerializeIrMode.INLINE,
    normalizeAbsolutePaths = false, sourceBaseDirs = emptyList()
) {
    init {
        assert(mode != JvmSerializeIrMode.NONE)
    }

    // Usage protocol: construct an instance, call only one of `serializeIrFile()` and `serializeTopLevelClass()` only once.

    fun serializeJvmIrFile(irFile: IrFile): JvmIr.ClassOrFile? {
        var anySaved = false
        val proto = JvmIr.ClassOrFile.newBuilder()

        declarationTable.inFile(irFile) {
            irFile.declarations.filter { it !is IrClass }.forEach { topDeclaration ->
                forEveryDeclarationToSerialize(topDeclaration, mode) { declaration ->
                    proto.addDeclaration(serializeDeclaration(declaration))
                    anySaved = true
                }
            }
        }
        if (!anySaved) return null

        serializeAuxTables(proto)
        proto.fileFacadeFqName = fileClassFqName.asString()

        return proto.build()
    }

    fun serializeTopLevelClass(irClass: IrClass): JvmIr.ClassOrFile? {
        val proto = JvmIr.ClassOrFile.newBuilder()
        declarationTable.inFile(irClass.parent as IrFile) {
            forEveryDeclarationToSerialize(irClass, mode) { declaration ->
                proto.addDeclaration(serializeDeclaration(declaration))
            }
        }
        serializeAuxTables(proto)
        proto.fileFacadeFqName = fileClassFqName.asString()

        return proto.build()
    }

    private fun serializeAuxTables(proto: JvmIr.ClassOrFile.Builder) {
        protoTypeArray.forEach(proto::addType)
        protoIdSignatureArray.forEach(proto::addSignature)
        protoStringArray.forEach(proto::addString)
        protoBodyArray.forEach { proto.addBody(it.toProto()) }
        protoDebugInfoArray.forEach(proto::addDebugInfo)
    }

    fun XStatementOrExpression.toProto(): JvmIr.XStatementOrExpression = when (this) {
        is XStatementOrExpression.XStatement -> JvmIr.XStatementOrExpression.newBuilder().setStatement(toProtoStatement()).build()
        is XStatementOrExpression.XExpression -> JvmIr.XStatementOrExpression.newBuilder().setExpression(toProtoExpression()).build()
    }
}

private fun forEveryDeclarationToSerialize(topDeclaration: IrDeclaration, mode: JvmSerializeIrMode, action: (IrDeclaration) -> Unit) {
    when (mode) {
        JvmSerializeIrMode.NONE -> error("should not even be called with serialization mode NONE")
        JvmSerializeIrMode.ALL -> action(topDeclaration)
        JvmSerializeIrMode.INLINE ->
            topDeclaration.accept(ForVisibleInlineFunctionsVisitor, action)
    }
}

private object ForVisibleInlineFunctionsVisitor : IrElementVisitor<Unit, (IrDeclaration) -> Unit> {
    override fun visitElement(element: IrElement, data: (IrDeclaration) -> Unit) {
        error("Visitor only for nonlocal declarations")
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: (IrDeclaration) -> Unit) {
        return
    }

    override fun visitClass(declaration: IrClass, data: (IrDeclaration) -> Unit) {
        if (!declaration.visibility.isVisibleOutside()) return
        for (child in declaration.declarations) {
            child.accept(this, data)
        }
    }

    override fun visitProperty(declaration: IrProperty, data: (IrDeclaration) -> Unit) {
        if (!declaration.visibility.isVisibleOutside()) return
        declaration.getter?.accept(this, data)
        declaration.setter?.accept(this, data)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: (IrDeclaration) -> Unit) {
        val action = data
        if (declaration.visibility.isVisibleOutside() &&
            declaration.isInline &&
            !declaration.isFakeOverride
        ) {
            action(declaration)
        }
    }
}
