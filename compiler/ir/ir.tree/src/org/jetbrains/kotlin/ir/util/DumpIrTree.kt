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
import org.jetbrains.kotlin.ir.SourceLocationManager
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCallExpression
import org.jetbrains.kotlin.ir.expressions.IrGetPropertyExpression
import org.jetbrains.kotlin.ir.expressions.IrSetPropertyExpression
import org.jetbrains.kotlin.ir.expressions.IrWhenExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
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

class DumpIrTreeVisitor(out: Appendable): IrElementVisitor<Unit, String> {
    val printer = Printer(out, "  ")
    val elementRenderer = RenderIrElementVisitor()

    override fun visitElement(element: IrElement, data: String) {
        element.dumpLabeledSubTree(data)
    }

    override fun visitCallExpression(expression: IrCallExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
            for (valueParameter in expression.descriptor.valueParameters) {
                expression.getArgument(valueParameter.index)?.accept(this, valueParameter.name.asString())
            }
        }
    }

    override fun visitGetProperty(expression: IrGetPropertyExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
        }
    }

    override fun visitSetProperty(expression: IrSetPropertyExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
            expression.value.accept(this, "\$value")
        }
    }

    override fun visitWhenExpression(expression: IrWhenExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.subject?.let { subject ->
                subject.accept(this, "\$subject:")
            }
            for (branch in expression.branches) {
                branch.condition.accept(this, "if")
                branch.result.accept(this, "then")
            }
            expression.elseExpression?.accept(this, "else")
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
        val fileEntry: SourceLocationManager.FileEntry,
        val lineNumber: Int,
        out: Appendable
): IrElementVisitor<Unit, Nothing?> {
    val dumper = DumpIrTreeVisitor(out)

    override fun visitElement(element: IrElement, data: Nothing?) {
        if (fileEntry.getLineNumber(element.startOffset) == lineNumber) {
            element.accept(dumper, "")
            return
        }

        element.acceptChildren(this, data)
    }
}
