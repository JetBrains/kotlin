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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
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
        element.dumpLabeledElementWith(data) {
            if (element is IrAnnotationContainer) {
                dumpAnnotations(element)
            }
            element.acceptChildren(this@DumpIrTreeVisitor, "")
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.files.dumpElements()
        }
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.fileAnnotations.dumpItemsWith("fileAnnotations") {
                ANNOTATIONS_RENDERER.renderAnnotation(it)
            }
            dumpAnnotations(declaration)
            declaration.declarations.dumpElements()
        }
    }

    override fun visitClass(declaration: IrClass, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.thisReceiver?.accept(this, "\$this")
            declaration.typeParameters.dumpElements()
            declaration.declarations.dumpElements()
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.correspondingProperty?.dumpInternal("correspondingProperty")
            declaration.overriddenSymbols.dumpItems<IrSymbol>("overridden") {
                it.dumpDeclarationElementOrDescriptor()
            }
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$this")
            declaration.extensionReceiverParameter?.accept(this, "\$receiver")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    private fun dumpAnnotations(element: IrAnnotationContainer) {
        element.annotations.dumpItems("annotations") {
            element.annotations.dumpElements()
        }
    }

    private fun IrSymbol.dumpDeclarationElementOrDescriptor(label: String? = null) {
        when {
            isBound ->
                owner.dumpInternal(label)
            label != null ->
                printer.println("$label: ", "UNBOUND: ", DescriptorRenderer.COMPACT.render(descriptor))
            else ->
                printer.println("UNBOUND: ", DescriptorRenderer.COMPACT.render(descriptor))
        }
    }

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.typeParameters.dumpElements()
            declaration.dispatchReceiverParameter?.accept(this, "\$outer")
            declaration.valueParameters.dumpElements()
            declaration.body?.accept(this, "")
        }
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.backingField?.accept(this, "")
            declaration.getter?.accept(this, "")
            declaration.setter?.accept(this, "")
        }
    }

    override fun visitField(declaration: IrField, data: String) {
        declaration.dumpLabeledElementWith(data) {
            dumpAnnotations(declaration)
            declaration.overriddenSymbols.dumpItems("overridden") {
                it.dumpDeclarationElementOrDescriptor()
            }
            declaration.initializer?.accept(this, "")
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
            dumpAnnotations(declaration)
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
        for (index in 0 until expression.typeArgumentsCount) {
            printer.println(
                "${expression.descriptor.renderTypeParameter(index)}: ${expression.renderTypeArgument(index)}"
            )
        }
    }

    private fun CallableDescriptor.renderTypeParameter(index: Int): String {
        val typeParameter = original.typeParameters.getOrNull(index)
        return if (typeParameter != null)
            DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(typeParameter)
        else
            "<`$index>"
    }

    private fun IrMemberAccessExpression.renderTypeArgument(index: Int): String =
        getTypeArgument(index)?.render() ?: "<none>"

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

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.typeOperandClassifier.dumpDeclarationElementOrDescriptor("typeOperand")
            expression.acceptChildren(this, "")
        }
    }

    private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented(body)
    }

    private inline fun <T> Collection<T>.dumpItems(caption: String, renderElement: (T) -> Unit) {
        if (isEmpty()) return
        indented(caption) {
            forEach {
                renderElement(it)
            }
        }
    }

    private inline fun <T> Collection<T>.dumpItemsWith(caption: String, renderElement: (T) -> String) {
        if (isEmpty()) return
        indented(caption) {
            forEach {
                printer.println(renderElement(it))
            }
        }
    }

    private fun IrElement.dumpInternal(label: String? = null) {
        if (label != null) {
            printer.println("$label: ", accept(elementRenderer, null))
        } else {
            printer.println(accept(elementRenderer, null))
        }

    }

    private inline fun indented(label: String, body: () -> Unit) {
        printer.println("$label:")
        indented(body)
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
