/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.utils.Printer

fun IrElement.dump(): String {
    val sb = StringBuilder()
    accept(DumpIrTreeVisitor(sb), "")
    return sb.toString()
}

fun IrFile.dumpTreesFromLineNumber(lineNumber: Int): String {
    val sb = StringBuilder()
    accept(DumpTreeFromSourceLineVisitor(fileEntry, lineNumber, sb), null)
    return sb.toString()
}

class DumpIrTreeVisitor(out: Appendable) : IrElementVisitor<Unit, String> {
    private val printer = Printer(out, "  ")
    private val elementRenderer = RenderIrElementVisitor()

    companion object {
        val ANNOTATIONS_RENDERER = DescriptorRenderer.withOptions {
            verbose = true
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
        }
    }

    override fun visitElement(element: IrElement, data: String) {
        element.dumpLabeledSubTree(data)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.files.dumpElements()
            declaration.externalPackageFragments.dumpElements()
        }
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.dumpLabeledElementWith(data) {
            if (declaration.fileAnnotations.isNotEmpty()) {
                printer.println("fileAnnotations:")
                indented {
                    declaration.fileAnnotations.forEach {
                        printer.println(ANNOTATIONS_RENDERER.renderAnnotation(it))
                    }
                }
            }
            declaration.declarations.dumpElements()
        }
    }

    override fun visitClass(declaration: IrClass, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.thisReceiver?.accept(this, "\$this")
            declaration.typeParameters.dumpElements()
            declaration.declarations.dumpElements()
        }
    }

    override fun visitFunction(declaration: IrFunction, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$this")
            declaration.extensionReceiverParameter?.accept(this, "\$receiver")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$outer")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.typeParameters.dumpElements()
            declaration.backingField?.accept(this, "")
            declaration.getter?.accept(this, "")
            declaration.setter?.accept(this, "")
        }
    }

    private fun List<IrElement>.dumpElements() {
        forEach { it.accept(this@DumpIrTreeVisitor, "") }
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.explicitReceiver?.accept(this, "receiver")
            expression.arguments.dumpElements()
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.initializerExpression?.accept(this, "init")
            declaration.correspondingClass?.accept(this, "class")
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            dumpTypeArguments(expression)
            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
            for (valueParameter in expression.descriptor.valueParameters) {
                expression.getValueArgument(valueParameter.index)?.accept(this, valueParameter.name.asString())
            }
        }
    }

    private fun dumpTypeArguments(expression: IrMemberAccessExpression) {
        for (typeParameter in expression.descriptor.original.typeParameters) {
            val renderedParameter = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(typeParameter)
            val typeArgument = expression.getTypeArgument(typeParameter)
            val renderedType = typeArgument?.let {
                DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(typeArgument)
            } ?: "null"
            printer.println("$renderedParameter: $renderedType")
        }
    }

    override fun visitGetField(expression: IrGetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
        }
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
            expression.value.accept(this, "value")
        }
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.branches.dumpElements()
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        branch.dumpLabeledElementWith(data) {
            branch.condition.accept(this, "if")
            branch.result.accept(this, "then")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.condition.accept(this, "condition")
            loop.body?.accept(this, "body")
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.body?.accept(this, "body")
            loop.condition.accept(this, "condition")
        }
    }

    override fun visitTry(aTry: IrTry, data: String) {
        aTry.dumpLabeledElementWith(data) {
            aTry.tryResult.accept(this, "try")
            aTry.catches.dumpElements()
            aTry.finallyExpression?.accept(this, "finally")
        }
    }

    private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented(body)
    }

    private fun IrElement.dumpLabeledSubTree(label: String) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented {
            acceptChildren(this@DumpIrTreeVisitor, "")
        }
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    private fun String.withLabel(label: String) =
        if (label.isEmpty()) this else "$label: $this"
}

class DumpTreeFromSourceLineVisitor(
    val fileEntry: SourceManager.FileEntry,
    private val lineNumber: Int,
    out: Appendable
) : IrElementVisitorVoid {
    private val dumper = DumpIrTreeVisitor(out)

    override fun visitElement(element: IrElement) {
        if (fileEntry.getLineNumber(element.startOffset) == lineNumber) {
            element.accept(dumper, "")
            return
        }

        element.acceptChildrenVoid(this)
    }
}
