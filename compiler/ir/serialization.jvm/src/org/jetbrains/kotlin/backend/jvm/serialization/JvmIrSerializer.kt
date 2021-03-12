/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.JvmSimpleFileClassInfo
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

class JvmIrSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    private val psiSourceManager: PsiSourceManager,
    externallyVisibleOnly: Boolean = true,
    skipExpects: Boolean = false,
) : IrFileSerializer(messageLogger, declarationTable, expectDescriptorToSymbol, externallyVisibleOnly, skipExpects) {

    // Usage protocol: construct an instance, call only one of `serializeIrFile()` and `serializeTopLevelClass()` only once.

    private class FacadeClassInfo(val signature: Int, val facadeName: Int)

    private val facadeInfoArray = mutableListOf<FacadeClassInfo>()

    fun serializeJvmIrFile(irFile: IrFile): JvmIr.JvmIrFile {
        val proto = JvmIr.JvmIrFile.newBuilder()

        irFile.declarations.filter { it !is IrClass }.forEach { declaration ->
            proto.addDeclaration(serializeDeclaration(declaration))
        }
        proto.addAllAnnotation(serializeAnnotations(irFile.annotations))


        val facadeFqName = irFile.facadeFqName()
        proto.addAllFacadeFqName(serializeFqName(facadeFqName.toString()))

        // TODO -- serialize referencesToTopLevelMap
        proto.auxTables = serializeAuxTables()

        return proto.build()
    }

    fun serializeTopLevelClass(irClass: IrClass): JvmIr.JvmIrClass {
        val proto = JvmIr.JvmIrClass.newBuilder()
        proto.irClass = serializeIrClass(irClass)
        proto.auxTables = serializeAuxTables()
        return proto.build()
    }

    override fun serializeIrSymbol(symbol: IrSymbol): Long {
        val declaration = symbol.owner as? IrDeclaration ?: error("Expected IrDeclaration: ${symbol.owner.render()}")
        if (declaration.parent is IrPackageFragment && declaration is IrMemberWithContainerSource && declaration !is IrClass) {
            val facadeClassName = declaration.facadeName() ?: return super.serializeIrSymbol(symbol)
            val facadeId = serializeName(facadeClassName)
            val signatureId = protoIdSignature(declaration)
            facadeInfoArray.add(FacadeClassInfo(signatureId, facadeId))
        }

        return super.serializeIrSymbol(symbol)
    }

    private fun serializeAuxTables(): JvmIr.AuxTables {
        val proto = JvmIr.AuxTables.newBuilder()
        protoTypeArray.forEach { proto.addType(it.toByteString()) }
        protoIdSignatureArray.forEach { proto.addSignature(it.toByteString()) }
        protoStringArray.forEach { proto.addString(ByteString.copyFromUtf8(it)) }
        protoBodyArray.forEach { proto.addBody(ByteString.copyFrom(it.toByteArray())) }
        facadeInfoArray.forEach {
            proto.addFacadeClassInfo(JvmIr.FacadeClassInfo.newBuilder().run {
                signature = it.signature
                facadeClassName = it.facadeName
                build()
            })
        }
        return proto.build()
    }

    fun IrFile.facadeFqName(): FqName {
        val fileClassInfo = when (val fileEntry = fileEntry) {
            is PsiSourceManager.PsiFileEntry -> {
                val ktFile = psiSourceManager.getKtFile(fileEntry)
                    ?: throw AssertionError("Unexpected file entry: $fileEntry")
                JvmFileClassUtil.getFileClassInfoNoResolve(ktFile)
            }
            is NaiveSourceBasedFileEntryImpl -> {
                JvmSimpleFileClassInfo(PackagePartClassUtils.getPackagePartFqName(fqName, fileEntry.name), false)
            }
            else -> error("unknown kind of file entry: $fileEntry")
        }
        return fileClassInfo.fileClassFqName
    }

    fun IrMemberWithContainerSource.facadeName(): Name? {
        val parent = parent
        if (this is IrClass || parent !is IrPackageFragment) return null
        return when (parent) {
            is IrFile -> parent.facadeFqName().shortName()
            is IrExternalPackageFragment -> {
                val source = containerSource as? JvmPackagePartSource ?: return null
                val facadeName = source.facadeClassName ?: source.className
                facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName()
            }
            else -> error("Unknown IrPackageFragment kind: $parent")
        }
    }
}



