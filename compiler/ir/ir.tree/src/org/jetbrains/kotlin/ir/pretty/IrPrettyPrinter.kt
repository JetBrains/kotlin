/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.util.capitalizeDecapitalize.*
import org.jetbrains.kotlin.utils.Printer
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction2

class IrPrettyPrinterOptions(
    internal val symbolContext: SymbolContext = SymbolContextImpl(),
    val printDebugInfo: Boolean = false,
    val omitPropertiesWithDefaultValues: Boolean = true,
)

fun IrElement.prettyPrint(options: IrPrettyPrinterOptions = IrPrettyPrinterOptions()) =
    prettyPrint(this, IrPrettyPrinter::printElement, options)

private inline fun <Target> prettyPrint(
    target: Target,
    print: IrPrettyPrinter.(Target) -> Unit,
    options: IrPrettyPrinterOptions,
): String {
    val sb = StringBuilder()
    IrPrettyPrinter(Printer(sb, 1, "  "), options).print(target)
    return sb.toString()
}

private class IrPrettyPrinter(private val printer: Printer, private val options: IrPrettyPrinterOptions) : IrElementVisitorVoid {

    fun printElement(element: IrElement) {
        element.acceptVoid(this)
    }

    override fun visitElement(element: IrElement) {
        // TODO: Throw an exception
        printer.println("// ${element::class.simpleName}")
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        printer.run {
            print(IrWorldBuilder::irModuleFragment.name)
            braces {
                declaration.acceptChildrenVoid(this@IrPrettyPrinter)
            }
        }
    }

    override fun visitFile(declaration: IrFile) {
        printer.run {
            print(IrModuleFragmentBuilder::irFile.name)
            parens {
                printStringLiteral(declaration.path)
            }
            braces {
                if (!declaration.fqName.isRoot) {
                    print(IrFileBuilder::packageName.name)
                    parens {
                        printStringLiteral(declaration.fqName)
                    }
                    println()
                }
                printAnnotations(declaration)
                declaration.acceptChildrenVoid(this@IrPrettyPrinter)
            }
        }
    }

    override fun visitClass(declaration: IrClass) {
        printer.run {
            print(IrDeclarationContainerBuilder::irClass.name)
            parens {
                printStringLiteral(declaration.name)
            }
            braces {
                printDebugInfo(declaration)
                printAnnotations(declaration)
                // TODO: Print origin
                // TODO: Print symbol
                printCall(
                    when (declaration.kind) {
                        ClassKind.CLASS -> IrClassBuilder::kindClass
                        ClassKind.INTERFACE -> IrClassBuilder::kindInterface
                        ClassKind.ENUM_CLASS -> IrClassBuilder::kindEnumClass
                        ClassKind.ENUM_ENTRY -> IrClassBuilder::kindEnumEntry
                        ClassKind.ANNOTATION_CLASS -> IrClassBuilder::kindAnnotationClass
                        ClassKind.OBJECT -> IrClassBuilder::kindObject
                    }
                )
                printVisibility(declaration)
                printModality(declaration.modality)
                printClassFlags(declaration)
                // TODO: Render supertypes
                declaration.acceptChildrenVoid(this@IrPrettyPrinter)
            }
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        super.visitSimpleFunction(declaration)
    }

    private fun printAnnotations(declaration: IrAnnotationContainer) {
        if (declaration.annotations.isEmpty()) return
        printer.run {
            print(IrFileBuilder::annotations.name)
            braces {
                for (annotation in declaration.annotations) {
                    annotation.acceptVoid(this@IrPrettyPrinter)
                }
            }
        }
    }

    private fun printDebugInfo(element: IrElement) {
        if (!options.printDebugInfo) return
        if (options.omitPropertiesWithDefaultValues && element.startOffset == UNDEFINED_OFFSET && element.endOffset == UNDEFINED_OFFSET) {
            return
        }
        fun offsetToString(offset: Int) = when (offset) {
            UNDEFINED_OFFSET -> ::UNDEFINED_OFFSET.name
            SYNTHETIC_OFFSET -> ::SYNTHETIC_OFFSET.name
            else -> offset.toString()
        }
        printer.run {
            print(IrElementBuilder<*>::debugInfo.name)
            parens {
                printWithSeparator {
                    item {
                        printWithNoIndent(offsetToString(element.startOffset))
                    }
                    item {
                        printWithNoIndent(offsetToString(element.endOffset))
                    }
                }
            }
            println()
        }
    }

    private fun printVisibility(declaration: IrDeclarationWithVisibility) {
        if (options.omitPropertiesWithDefaultValues && declaration.visibility == DescriptorVisibilities.DEFAULT_VISIBILITY) return
        val method = when (declaration.visibility) {
            DescriptorVisibilities.PRIVATE -> IrDeclarationWithVisibilityBuilder::visibilityPrivate
            DescriptorVisibilities.PRIVATE_TO_THIS -> IrDeclarationWithVisibilityBuilder::visibilityPrivateToThis
            DescriptorVisibilities.PROTECTED -> IrDeclarationWithVisibilityBuilder::visibilityProtected
            DescriptorVisibilities.INTERNAL -> IrDeclarationWithVisibilityBuilder::visibilityInternal
            DescriptorVisibilities.PUBLIC -> IrDeclarationWithVisibilityBuilder::visibilityPublic
            DescriptorVisibilities.LOCAL -> IrDeclarationWithVisibilityBuilder::visibilityLocal
            DescriptorVisibilities.INHERITED -> IrDeclarationWithVisibilityBuilder::visibilityInherited
            DescriptorVisibilities.INVISIBLE_FAKE -> IrDeclarationWithVisibilityBuilder::visibilityInvisibleFake
            DescriptorVisibilities.UNKNOWN -> IrDeclarationWithVisibilityBuilder::visibilityUnknown
            else -> {
                printer.printCall(
                    "visibility" + declaration.visibility.name.decapitalizeSmart(asciiOnly = true).replaceFirstChar { it.uppercaseChar() })
                return
            }
        }
        printer.printCall(method)
    }

    private fun printModality(modality: Modality) {
        if (options.omitPropertiesWithDefaultValues && modality == Modality.FINAL) return
        val method = when (modality) {
            Modality.FINAL -> IrDeclarationWithModalityBuilder::modalityFinal
            Modality.SEALED -> IrDeclarationWithModalityBuilder::modalitySealed
            Modality.OPEN -> IrDeclarationWithModalityBuilder::modalityOpen
            Modality.ABSTRACT -> IrDeclarationWithModalityBuilder::modalityAbstract
        }
        printer.printCall(method)
    }

    private fun printClassFlags(declaration: IrClass) {
        declaration.run {
            printFlagList(
                IrClassBuilder::companion.takeIf { isCompanion },
                IrClassBuilder::inner.takeIf { isInner },
                IrClassBuilder::data.takeIf { isData },
                IrClassBuilder::external.takeIf { isExternal },
                IrClassBuilder::value.takeIf { isValue },
                IrClassBuilder::expect.takeIf { isExpect },
                IrClassBuilder::functional.takeIf { isFun }
            )
        }
    }

    private fun <Builder : IrElementBuilder<*>> printFlagList(vararg methods: KFunction2<Builder, Boolean, Unit>?) {
        for (method in methods) {
            method?.let(printer::printCall)
        }
    }
}

private inline fun Printer.indented(block: () -> Unit) {
    pushIndent()
    try {
        block()
    } finally {
        popIndent()
    }
}

private inline fun Printer.braces(block: () -> Unit) {
    printlnWithNoIndent(" {")
    indented(block)
    println("}")
}

private inline fun Printer.parens(block: () -> Unit) {
    printWithNoIndent("(")
    try {
        block()
    } finally {
        printWithNoIndent(")")
    }
}

private fun Printer.printCall(methodName: String) {
    print(methodName)
    parens {}
    printlnWithNoIndent()
}

private fun Printer.printCall(method: KFunction<*>) {
    printCall(method.name)
}

private class PrintWithSeparator(private val separator: String, private val printer: Printer) {
    private var isFirst = true

    inline fun item(block: Printer.() -> Unit) {
        if (!isFirst) printer.printWithNoIndent(separator)
        printer.block()
        isFirst = false
    }
}

private inline fun Printer.printWithSeparator(
    separator: String = ", ",
    block: PrintWithSeparator.() -> Unit
) {
    PrintWithSeparator(separator, this).block()
}

private fun Printer.printStringLiteral(s: Any) {
    // FIXME: Properly escape!
    printWithNoIndent('"')
    printWithNoIndent(s.toString())
    printWithNoIndent('"')
}
