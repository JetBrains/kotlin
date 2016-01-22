/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.console.gutter

import com.intellij.openapi.editor.markup.GutterIconRenderer
import org.jetbrains.kotlin.console.SeverityDetails
import org.jetbrains.kotlin.diagnostics.Severity

class ConsoleErrorRenderer(private val messages: List<SeverityDetails>) : GutterIconRenderer() {
    private fun msgType(severity: Severity) = when (severity) {
        Severity.ERROR -> "Error:"
        Severity.WARNING -> "Warning:"
        Severity.INFO -> "Info:"
    }

    override fun getTooltipText(): String {
        val htmlTooltips = messages.map { "<b>${msgType(it.severity)}</b> ${it.description}" }
        return "<html>${htmlTooltips.joinToString("<hr size=1 noshade>")}</html>"
    }

    override fun getIcon() = ReplIcons.COMPILER_ERROR
    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = this === other
}