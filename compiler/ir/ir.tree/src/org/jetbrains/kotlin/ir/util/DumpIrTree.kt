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
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.Printer

fun IrElement.dump(): String =
    StringBuilder().also { sb ->
        accept(DumpIrTreeVisitor(sb), "")
    }.toString()

fun IrFile.dumpTreesFromLineNumber(lineNumber: Int): String {
    val sb = StringBuilder()
    accept(DumpTreeFromSourceLineVisitor(fileEntry, lineNumber, sb), null)
    return sb.toString()
}

class DumpIrTreeVisitor(
    out: Appendable
) : IrElementVisitor<Unit, String> {

    private val printer = Printer(out, "  ")
    private val elementRenderer = RenderIrElementVisitor()
    private fun IrType.render() = elementRenderer.renderType(this)

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
            declaration.correspondingPropertySymbol?.dumpInternal("correspondingProperty")
            declaration.overriddenSymbols.dumpItems<IrSymbol>("overridden") {
                it.dump()
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

    private fun IrSymbol.dump(label: String? = null) =
        printer.println(
            elementRenderer.renderSymbolReference(this).let {
                if (label != null) "$label: $it" else it
            }
        )

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
                it.dump()
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
            val valueParameterNames = expression.getValueParameterNames(expression.valueArgumentsCount)
            for (index in 0 until expression.valueArgumentsCount) {
                expression.getValueArgument(index)?.accept(this, valueParameterNames[index])
            }
        }
    }

    private fun dumpTypeArguments(expression: IrMemberAccessExpression) {
        val typeParameterNames = expression.getTypeParameterNames(expression.typeArgumentsCount)
        for (index in 0 until expression.typeArgumentsCount) {
            printer.println(
                "<${typeParameterNames[index]}>: ${expression.renderTypeArgument(index)}"
            )
        }
    }

    private fun IrMemberAccessExpression.getTypeParameterNames(expectedCount: Int): List<String> =
        if (this is IrDeclarationReference && symbol.isBound)
            symbol.owner.getTypeParameterNames(expectedCount)
        else if (this is IrCallableReference)
            getPlaceholderParameterNames(expectedCount) // TODO IrCallableReference should be an IrDeclarationReference
        else
            getPlaceholderParameterNames(expectedCount)

    private fun IrMemberAccessExpression.getValueParameterNames(expectedCount: Int): List<String> =
        if (this is IrDeclarationReference && symbol.isBound)
            symbol.owner.getValueParameterNames(expectedCount)
        else if (this is IrCallableReference)
            getPlaceholderParameterNames(expectedCount) // TODO IrCallableReference should be an IrDeclarationReference
        else
            getPlaceholderParameterNames(expectedCount)

    private fun getPlaceholderParameterNames(expectedCount: Int) =
        (1..expectedCount).map { "$it" }

    private fun IrSymbolOwner.getTypeParameterNames(expectedCount: Int): List<String> =
        if (this is IrTypeParametersContainer) {
            val typeParameters = if (this is IrConstructor) getFullTypeParametersList() else this.typeParameters
            (0 until expectedCount).map {
                if (it < typeParameters.size)
                    typeParameters[it].name.asString()
                else
                    "${it + 1}"
            }
        } else {
            getPlaceholderParameterNames(expectedCount)
        }

    private fun IrSymbolOwner.getValueParameterNames(expectedCount: Int): List<String> =
        if (this is IrFunction) {
            (0 until expectedCount).map {
                if (it < valueParameters.size)
                    valueParameters[it].name.asString()
                else
                    "${it + 1}"
            }
        } else {
            getPlaceholderParameterNames(expectedCount)
        }

    private fun IrConstructor.getFullTypeParametersList(): List<IrTypeParameter> =
        getConstructedClassTypeParameters().apply { addAll(typeParameters) }

    private fun IrConstructor.getConstructedClassTypeParameters(): MutableList<IrTypeParameter> {
        val typeParameters = ArrayList<IrTypeParameter>()
        val parentClass = try {
            parent as? IrClass ?: return typeParameters
        } catch (e: Exception) {
            return typeParameters
        }
        parentClass.collectClassTypeParameters(typeParameters)
        return typeParameters
    }

    private fun IrClass.collectClassTypeParameters(typeParameters: MutableList<IrTypeParameter>) {
        var currentClass = this
        while (true) {
            typeParameters.addAll(currentClass.typeParameters)
            if (!currentClass.isInner) return
            currentClass = try {
                currentClass.parent as? IrClass ?: return
            } catch (e: Exception) {
                return
            }
        }
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
            expression.acceptChildren(this, "")
        }
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver.accept(this, "receiver")
            for ((i, arg) in expression.arguments.withIndex()) {
                arg.accept(this, i.toString())
            }
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

    private fun IrSymbol.dumpInternal(label: String? = null) {
        if (isBound)
            owner.dumpInternal(label)
        else
            printer.println("$label: UNBOUND ${javaClass.simpleName}")
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
