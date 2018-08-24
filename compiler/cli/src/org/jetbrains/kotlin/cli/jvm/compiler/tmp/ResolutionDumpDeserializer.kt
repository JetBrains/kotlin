/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.*
import java.io.File

interface ResolutionDumpDeserializer {
    fun deserializeResolutionResults(bytes: ByteArray): RDumpForFile
}

class ProtoBufResolutionDumpDeserializer(private val root: File) : ResolutionDumpDeserializer {
    override fun deserializeResolutionResults(bytes: ByteArray): RDumpForFile {
        val proto: ResolutionDumpProtoBuf.ResolutionResultForFile = ResolutionDumpProtoBuf.ResolutionResultForFile.parseFrom(bytes)

        return loadDumpForFile(proto)
    }

    private fun loadDumpForFile(dumpForFileProto: ResolutionDumpProtoBuf.ResolutionResultForFile): RDumpForFile {
        val file = loadFile(dumpForFileProto.file)

        val elements = dumpForFileProto.elementList.map { loadElement(it) }
        return RDumpForFile(file, elements)
    }

    private fun loadElement(elementProto: ResolutionDumpProtoBuf.Element): RDumpElement {
        val offset = elementProto.offset
        val presentableText = elementProto.presentableText

        return when {
            elementProto.hasExpression() -> loadExpression(offset, presentableText, elementProto.expression)
            else -> throw IllegalStateException("Unknown deserialized resolution dump element") // TODO: do we need better error handling here?
        }
    }

    private fun loadExpression(offset: Int, presentableText: String, expressionProto: ResolutionDumpProtoBuf.Expression): RDumpExpression {
        val type = loadType(expressionProto.type)

        return when {
            expressionProto.hasError() -> loadError(offset, presentableText, expressionProto.error)
            expressionProto.hasResolved() -> loadResolved(offset, presentableText, type, expressionProto.resolved)
            else -> throw IllegalStateException("Unknown deserialized resolution dump element") // TODO: do we need better error handling here?
        }
    }

    private fun loadResolved(
        offset: Int,
        presentableText: String,
        type: RDumpType,
        resolvedProto: ResolutionDumpProtoBuf.Resolved
    ): RDumpExpression {
        val typeArguments = resolvedProto.typeArgumentList.map { loadType(it) }
        val descriptor = loadDescriptor(resolvedProto.candidateDescriptor)

        return RDumpResolvedCall(offset, presentableText, type, descriptor, typeArguments)
    }

    private fun loadDescriptor(descriptorProto: ResolutionDumpProtoBuf.Descriptor): RDumpDescriptor =
        TextBasedRDumpDescriptor(
            descriptorProto.fqn,
            descriptorProto.typeParameterList.map { loadDescriptor(it) }
        )

    private fun loadError(offset: Int, presentableText: String, errorProto: ResolutionDumpProtoBuf.Error): RDumpErrorCall {
        val diagnostics = errorProto.diagnosticList.map { loadDiagnostic(it) }
        return RDumpErrorCall(offset, presentableText, diagnostics)
    }

    private fun loadDiagnostic(diagnosticProto: ResolutionDumpProtoBuf.Diagnostic): RDumpDiagnostic {
        return TextBasedRDumpDiagnostic(diagnosticProto.diagnosticFamily, diagnosticProto.renderedText)
    }

    private fun loadType(type: ResolutionDumpProtoBuf.Type): RDumpType {
        return TextBasedRDumpType(type.renderedType)
    }

    private fun loadFile(fileProto: ResolutionDumpProtoBuf.File): RDumpFile {
        val file = File(root, fileProto.relativePath)
        if (!file.exists()) {
            throw IllegalStateException("Deserialized File with relative path ${fileProto.relativePath} hasn't been resolved relative to root=$root")
        }

        return FileBasedRDumpFile(file, fileProto.relativePath)
    }
}