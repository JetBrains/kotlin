/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.FileBasedRDumpFile
import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.RDumpResolvedCall
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.ErrorType
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import java.io.File

class IrBasedResolutionDumpBuilder(
    private val bindingContext: BindingContext,
    private val nativeElementsFactory: NativeElementsFactory,
    private val root: File
) {
    fun buildResolutionDumpForFile(irFile: IrFile, correspondingPhysicalFile: File): RDumpForFile {
        val results = Ir2ResolutionDumpConverter().collectResolutionResultsFromSubtree(irFile)
        return RDumpForFile(
            FileBasedRDumpFile(correspondingPhysicalFile, correspondingPhysicalFile.toRelativeString(root)),
            results
        )
    }

    inner class Ir2ResolutionDumpConverter : IrElementVisitorVoid {
        private val results: MutableList<RDumpElement> = mutableListOf()

        fun collectResolutionResultsFromSubtree(irElement: IrElement): List<RDumpElement> {
            irElement.accept(this, null)
            return results
        }


        // === Visitor ===

        override fun visitElement(element: IrElement) {
            throw IllegalStateException("Shouldn't visit $element")
        }

        override fun visitDeclaration(declaration: IrDeclaration) {
            declaration.acceptChildren(this, null)
        }

        override fun visitBody(body: IrBody) {
            body.acceptChildren(this, null)
        }

        // Expressions
        override fun visitCall(expression: IrCall) {
            val descriptor = expression.descriptor
            val rDumpDescriptor = nativeElementsFactory.convertToRDumpDescriptor(descriptor)
            val rDumpType = nativeElementsFactory.convertToRDumpType(expression.type)

            val typeArguments: MutableList<RDumpType> = mutableListOf()
            for ((i, typeParameter) in descriptor.typeParameters.withIndex()) {
                val kotlinTypeArgument = expression.getTypeArgument(i)
                        ?: ErrorUtils.createErrorType("Error type for typeParameter ${DescriptorRenderer.COMPACT.render(typeParameter)}")
                typeArguments.add(nativeElementsFactory.convertToRDumpType(kotlinTypeArgument))
            }

            results.add(RDumpResolvedCall(expression.startOffset, expression.dump(), rDumpType, rDumpDescriptor, typeArguments))
        }
    }

    inner class AdditionalDiagnosticInfoProvider {
    }
}