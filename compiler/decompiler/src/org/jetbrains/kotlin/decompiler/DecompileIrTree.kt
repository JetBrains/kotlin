/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import org.jetbrains.kotlin.decompiler.util.DecompilerIrElementVisitor
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

fun IrElement.decompile(): String =
    StringBuilder().also { sb ->
        accept(DecompileIrTreeVisitor(sb), "")
    }.toString()

fun IrFile.decompileTreesFromLineNumber(lineNumber: Int): String {
    val sb = StringBuilder()
    accept(DecompileTreeFromSourceLineVisitor(fileEntry, lineNumber, sb), null)
    return sb.toString()
}

class DecompileIrTreeVisitor(
    out: Appendable
) : IrElementVisitor<Unit, String> {

    private val printer = Printer(out, "    ")
    private val elementRenderer = DecompilerIrElementVisitor()
    private fun IrType.render() = elementRenderer.renderType(this)

    override fun visitElement(element: IrElement, data: String) {
//        element.dumpLabeledElementWith(data) {
            if (element is IrAnnotationContainer) {
                decompileAnnotations(element)
            }
            element.acceptChildren(this@DecompileIrTreeVisitor, "")
//        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) = TODO()

    override fun visitFile(declaration: IrFile, data: String) {
        decompileAnnotations(declaration)
        declaration.declarations.decompileElements()
    }

    override fun visitBlockBody(body: IrBlockBody, data: String) {
        with(printer) {
            println(" {")
//            pushIndent()
            body.statements.decompileElements()
//            popIndent()
            println("}")
            println()
        }
    }

    override fun visitReturn(returnStatement: IrReturn, data: String) {
        with(printer) {
            pushIndent()
            print("return ")
            popIndent()
            returnStatement.value.accept(this@DecompileIrTreeVisitor, "")
        }
    }

    override fun visitClass(declaration: IrClass, data: String) = TODO()

    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) = TODO()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) = TODO()

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        declaration.dumpLabeledElementWith(data) {
            decompileAnnotations(declaration)
//            declaration.correspondingPropertySymbol?.dumpInternal("correspondingProperty")
//            declaration.overriddenSymbols.dumpItems<IrSymbol>("overridden") {
//                it.decompile()
//            }
//            declaration.typeParameters.decompileElements()
//            declaration.dispatchReceiverParameter?.accept(this, "\$this")
//            declaration.extensionReceiverParameter?.accept(this, "\$receiver")
//            declaration.valueParameters.decompileElements()
            declaration.body?.accept(this, "")
        }
    }

    private fun decompileAnnotations(element: IrAnnotationContainer) {
        //TODO правильно рендерить аннотации
    }

    override fun visitConstructor(declaration: IrConstructor, data: String) = TODO()

    override fun visitProperty(declaration: IrProperty, data: String) = TODO()

    override fun visitField(declaration: IrField, data: String) = TODO()

    private fun List<IrElement>.decompileElements() {
        forEach { it.accept(this@DecompileIrTreeVisitor, "") }
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) = TODO()

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) = TODO()

    override fun visitCall(expression: IrCall, data: String) {
        dumpTypeArguments(expression)
        if (expression.dispatchReceiver != null) {
            expression.dispatchReceiver?.accept(this, "")
            printer.print(".")
        }
        printer.print("${expression.symbol.owner.name}(")
        for (index in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(index)?.accept(this, "")
            if (index != expression.valueArgumentsCount - 1) printer.print(", ")
        }
        printer.println(")")

    }

    override fun visitGetValue(expression: IrGetValue, data: String) {
        printer.print(expression.symbol.owner.name)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) = TODO()
    override fun visitConstructorCall(expression: IrConstructorCall, data: String) = TODO()

    private fun dumpTypeArguments(expression: IrMemberAccessExpression) {
        val typeParameterNames = expression.getTypeParameterNames(expression.typeArgumentsCount)
        for (index in 0 until expression.typeArgumentsCount) {
            printer.println("<${typeParameterNames[index]}>: ${expression.renderTypeArgument(index)}")
        }
    }

    private fun IrMemberAccessExpression.getTypeParameterNames(expectedCount: Int): List<String> =
        if (this is IrDeclarationReference && symbol.isBound)
            symbol.owner.getTypeParameterNames(expectedCount)
        else
            getPlaceholderParameterNames(expectedCount)

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

    private fun IrConstructor.getFullTypeParametersList(): List<IrTypeParameter> {
        val parentClass = try {
            parent as? IrClass ?: return typeParameters
        } catch (e: Exception) {
            return typeParameters
        }
        return parentClass.typeParameters + typeParameters
    }

    private fun IrMemberAccessExpression.renderTypeArgument(index: Int): String =
        getTypeArgument(index)?.render() ?: "<none>"

    override fun visitGetField(expression: IrGetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
        }
    }

    override fun visitSetField(expression: IrSetField, data: String) = TODO()

    override fun visitWhen(expression: IrWhen, data: String) = TODO()

    override fun visitBranch(branch: IrBranch, data: String) = TODO()

    override fun visitWhileLoop(loop: IrWhileLoop, data: String) = TODO()

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) = TODO()

    override fun visitTry(aTry: IrTry, data: String) = TODO()

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) = TODO()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) = TODO()

    private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
        printer.print(accept(elementRenderer, null).withLabel(label))
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
//        printer.pushIndent()
        body()
//        printer.popIndent()
    }

    private fun String.withLabel(label: String) =
        if (label.isEmpty()) this else "$label: $this"
}

class DecompileTreeFromSourceLineVisitor(
    val fileEntry: SourceManager.FileEntry,
    private val lineNumber: Int,
    out: Appendable
) : IrElementVisitorVoid {
    private val dumper = DecompileIrTreeVisitor(out)

    override fun visitElement(element: IrElement) {
        if (fileEntry.getLineNumber(element.startOffset) == lineNumber) {
            element.accept(dumper, "")
            return
        }

        element.acceptChildrenVoid(this)
    }
}

internal fun IrMemberAccessExpression.getValueParameterNamesForDebug(): List<String> {
    val expectedCount = valueArgumentsCount
    return if (this is IrDeclarationReference && symbol.isBound) {
        val owner = symbol.owner
        if (owner is IrFunction) {
            (0 until expectedCount).map {
                if (it < owner.valueParameters.size)
                    owner.valueParameters[it].name.asString()
                else
                    "${it + 1}"
            }
        } else {
            getPlaceholderParameterNames(expectedCount)
        }
    } else
        getPlaceholderParameterNames(expectedCount)
}

internal fun getPlaceholderParameterNames(expectedCount: Int) =
    (1..expectedCount).map { "$it" }
