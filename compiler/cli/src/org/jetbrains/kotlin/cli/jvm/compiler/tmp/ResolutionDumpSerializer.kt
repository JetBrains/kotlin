/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.*

interface ResolutionDumpSerializer {
    fun serialize(dumpForFile: RDumpForFile): ByteArray
}

class ProtoBufResolutionDumpSerializer : ResolutionDumpSerializer {
    override fun serialize(dumpForFile: RDumpForFile): ByteArray {
        return resolutionResultsForFileProto(dumpForFile).build().toByteArray()
    }

    private fun resolutionResultsForFileProto(dumpForFile: RDumpForFile): ResolutionDumpProtoBuf.ResolutionResultForFile.Builder {
        return ResolutionDumpProtoBuf.ResolutionResultForFile.newBuilder().apply {
            file = ResolutionDumpProtoBuf.File.newBuilder().setRelativePath(dumpForFile.ownerFile.pathRelativeToRoot).build()

            for (rDumpElement in dumpForFile.results) {
                val elementBuilder = ResolutionDumpProtoBuf.Element.newBuilder()
                elementBuilder.offset = rDumpElement.offset
                elementBuilder.presentableText = rDumpElement.presentableText

                when (rDumpElement) {
                    is RDumpExpression ->
                        elementBuilder.expression = expressionProto(rDumpElement).build()

                    else -> throw IllegalStateException("Unknown Element type: $rDumpElement")
                }

                addElement(elementBuilder.build())
            }
        }
    }

    private fun expressionProto(expression: RDumpExpression): ResolutionDumpProtoBuf.Expression.Builder {
        return ResolutionDumpProtoBuf.Expression.newBuilder().apply {
            type = typeProto(expression.type).build()

            when (expression) {
                is RDumpResolvedCall -> resolved = resolvedProto(expression).build()
                is RDumpErrorCall -> error = errorProto(expression).build()
                else -> throw IllegalStateException("Unknown Expression type: $expression")
            }
        }
    }

    private fun resolvedProto(resolved: RDumpResolvedCall): ResolutionDumpProtoBuf.Resolved.Builder {
        val candidateDescriptorProto = descriptorProto(resolved.candidateDescriptor).build()
        val typeArgumentsProtos = resolved.typeArguments.map { typeProto(it).build() }

        return ResolutionDumpProtoBuf.Resolved.newBuilder()
            .setCandidateDescriptor(candidateDescriptorProto)
            .addAllTypeArgument(typeArgumentsProtos)
    }

    private fun errorProto(error: RDumpErrorCall): ResolutionDumpProtoBuf.Error.Builder {
        return ResolutionDumpProtoBuf.Error.newBuilder().apply {
            for (diagnostic in error.diagnostics) {
                addDiagnostic(diagnosticProto(diagnostic))
            }
        }
    }

    private fun diagnosticProto(rDumpDiagnostic: RDumpDiagnostic): ResolutionDumpProtoBuf.Diagnostic.Builder {
        val textBasedRDumpDiagnostic = rDumpDiagnostic as TextBasedRDumpDiagnostic
        return ResolutionDumpProtoBuf.Diagnostic.newBuilder().setRenderedText(textBasedRDumpDiagnostic.fullRenderedText)
    }

    private fun descriptorProto(rDumpDescriptor: RDumpDescriptor): ResolutionDumpProtoBuf.Descriptor.Builder {
        return ResolutionDumpProtoBuf.Descriptor.newBuilder().apply {
            fqn = (rDumpDescriptor as? TextBasedRDumpDescriptor)?.renderedDescriptor

            for (typeParameterDescriptor in rDumpDescriptor.typeParameters) {
                addTypeParameter(descriptorProto(typeParameterDescriptor).build())
            }
        }
    }

    private fun typeProto(rDumpType: RDumpType): ResolutionDumpProtoBuf.Type.Builder =
        ResolutionDumpProtoBuf.Type.newBuilder().setRenderedType((rDumpType as TextBasedRDumpType).renderedType)
}