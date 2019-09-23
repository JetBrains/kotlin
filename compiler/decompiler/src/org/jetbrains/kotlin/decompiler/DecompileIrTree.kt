/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import org.jetbrains.kotlin.decompiler.util.DecompilerIrElementVisitor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.primaryConstructor
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
    private val elementDecompiler = DecompilerIrElementVisitor()
    private fun IrType.render() = elementDecompiler.renderType(this)

    override fun visitElement(element: IrElement, data: String) {
        element.generatesSourcesForElement {
            if (element is IrAnnotationContainer) {
                decompileAnnotations(element)
            }
            element.acceptChildren(this@DecompileIrTreeVisitor, "")
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.files.decompileElements()
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.declarations.decompileElements()
    }

    override fun visitBlockBody(body: IrBlockBody, data: String) {
        withBraces {
            body.statements
                .filterNot { it is IrDelegatingConstructorCall }
                .filterNot { it is IrInstanceInitializerCall }
                .decompileElements()
        }
    }

    override fun visitBlock(expression: IrBlock, data: String) {
        expression.statements.decompileElements()
    }


    override fun visitReturn(expression: IrReturn, data: String) {
        printer.println("${expression.accept(elementDecompiler, null)} ${expression.value.accept(elementDecompiler, null)}")
    }

    private fun IrElement.decompile(): String {
        return accept(elementDecompiler, null)
    }

    override fun visitClass(declaration: IrClass, data: String) {
        printer.print(declaration.accept(elementDecompiler, null))
        if (declaration.kind == ClassKind.CLASS) {
//            printer.print(declaration.primaryConstructor?.decompile())
            withBraces {
                declaration.declarations
//                    .filterNot { it is IrConstructor && it.isPrimary }
                    .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                    .decompileElements()
            }
        } else {
            withBraces {
                declaration.declarations
                    .filterNot { it is IrConstructor }
                    .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                    .decompileElements()
            }
        }

    }

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        printer.print(declaration.accept(elementDecompiler, null))
        declaration.body?.accept(this, "")
    }


    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) = TODO()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) = TODO()

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        declaration.generatesSourcesForElement {
            decompileAnnotations(declaration)
            declaration.body?.accept(this, "")
        }
        printer.printlnWithNoIndent()
    }

    private fun decompileAnnotations(element: IrAnnotationContainer) {
        //TODO правильно рендерить аннотации
    }

    override fun visitVariable(declaration: IrVariable, data: String) {
        printer.println(declaration.decompile())
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        printer.println(declaration.decompile())
        indented {
            declaration.getter?.decompile()
            declaration.setter?.decompile()
        }
    }

    override fun visitField(declaration: IrField, data: String) = TODO()

    private fun List<IrElement>.decompileElements() {
        forEach { it.accept(this@DecompileIrTreeVisitor, "") }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: String) {
        printer.println(expression.decompile())
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: String) {
        printer.println(expression.decompile())
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        printer.print(expression.decompile())
        withBraces {
            expression.branches.decompileElements()
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        printer.print("${branch.condition.decompile()} -> ")
        withBraces {
            branch.result.accept(this, "")
        }
    }

    override fun visitElseBranch(branch: IrElseBranch, data: String) {
        printer.print("else -> ")
        withBraces {
            branch.result.accept(this, "")
        }

    }

    override fun visitSetVariable(expression: IrSetVariable, data: String) {
        printer.println(expression.decompile())
    }


    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) = TODO()
    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) = TODO()
    override fun visitGetValue(expression: IrGetValue, data: String) = TODO()
    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) = TODO()
    override fun visitConstructorCall(expression: IrConstructorCall, data: String) = TODO()
    override fun visitGetField(expression: IrGetField, data: String) = TODO()
    override fun visitSetField(expression: IrSetField, data: String) = TODO()


    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        loop.generatesSourcesForElement {
            withBraces {
                loop.body?.accept(this@DecompileIrTreeVisitor, "")
            }
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        printer.println("do {\n")
        indented {
            loop.body?.accept(this@DecompileIrTreeVisitor, "")
        }
        printer.println("} ${loop.decompile()}")
    }

    override fun visitComposite(expression: IrComposite, data: String) {
        expression.statements.decompileElements()
    }


    override fun visitTry(aTry: IrTry, data: String) = TODO()
    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) = TODO()
    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) = TODO()

    private inline fun IrElement.generatesSourcesForElement(body: () -> Unit) {
        printer.print("${decompile()} ")
        body()
    }

    private inline fun withBraces(body: () -> Unit) {
        printer.printlnWithNoIndent("{")
        indented(body)
        printer.println("} ")
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }
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
