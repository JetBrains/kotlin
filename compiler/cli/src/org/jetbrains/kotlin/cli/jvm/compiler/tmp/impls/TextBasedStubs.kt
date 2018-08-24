/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.NativeElementsFactory
import org.jetbrains.kotlin.cli.jvm.compiler.tmp.RDumpDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.tmp.RDumpDiagnostic
import org.jetbrains.kotlin.cli.jvm.compiler.tmp.RDumpType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType

open class TextBasedRDumpType(val renderedType: String) : RDumpType
object RDumpErrorType : TextBasedRDumpType("ERROR_TYPE")

open class TextBasedRDumpDescriptor(val renderedDescriptor: String, override val typeParameters: List<RDumpDescriptor>) : RDumpDescriptor

class TextBasedRDumpDiagnostic(val diagnosticFamily: String, val fullRenderedText: String) : RDumpDiagnostic

object TextBasedNativeElementsFactory : NativeElementsFactory {
    override fun convertToRDumpDescriptor(descriptor: DeclarationDescriptor): RDumpDescriptor {
        val typeParameters = (descriptor as? CallableDescriptor)?.typeParameters?.map { convertToRDumpDescriptor(it) } ?: emptyList()
        return TextBasedRDumpDescriptor(renderDescriptor(descriptor), typeParameters)
    }

    override fun convertToRDumpType(type: KotlinType): RDumpType {
        return TextBasedRDumpType(renderType(type))
    }

    override fun convertToRDumpDiagnostic(diagnostic: Diagnostic): RDumpDiagnostic {
        return TextBasedRDumpDiagnostic(
            diagnosticFamily = diagnostic.factory.name,
            fullRenderedText = renderDiagnostic(diagnostic)
        )
    }

    private fun renderDescriptor(descriptor: DeclarationDescriptor): String {
        return DescriptorRenderer.DEBUG_TEXT.render(descriptor)
    }

    private fun renderType(type: KotlinType): String {
        return DescriptorRenderer.DEBUG_TEXT.renderType(type)
    }

    private fun renderDiagnostic(diagnostic: Diagnostic): String {
        return DefaultErrorMessages.render(diagnostic)
    }
}