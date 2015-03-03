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

package org.jetbrains.kotlin.android

import com.android.ide.common.blame.output.GradleMessage
import com.android.ide.common.blame.parser.PatternAwareOutputParser
import com.android.ide.common.blame.parser.util.OutputLineReader
import com.android.utils.ILogger
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.util.regex.Pattern

public class KotlinOutputParser : PatternAwareOutputParser {

    override fun parse(line: String, reader: OutputLineReader, messages: MutableList<GradleMessage>, logger: ILogger): Boolean {
        val colonIndex1 = line.colon()
        val severity = if (colonIndex1 >= 0) line.substringBeforeAndTrim(colonIndex1).parseSeverity() else null
        if (severity == null) return false

        val lineWoSeverity = line.substringAfterAndTrim(colonIndex1)
        val colonIndex2 = lineWoSeverity.colon().skipDriveOnWin(lineWoSeverity)
        if (colonIndex2 >= 0) {
            val path = lineWoSeverity.substringBeforeAndTrim(colonIndex2)
            val file = File(path)
            if (!file.isFile() && FileUtilRt.getExtension(file.getName()) != "kt") {
                addMessage(GradleMessage(severity, lineWoSeverity.amendNextLinesIfNeeded(reader)), messages)
                return true
            }

            val lineWoPath = lineWoSeverity.substringAfterAndTrim(colonIndex2)
            val colonIndex3 = lineWoPath.colon()
            if (colonIndex3 >= 0) {
                val position = lineWoPath.substringBeforeAndTrim(colonIndex3)

                val matcher = POSITION_PATTERN.matcher(position)
                val message = lineWoPath.substringAfterAndTrim(colonIndex3).amendNextLinesIfNeeded(reader)

                if (matcher.matches()) {
                    val lineNumber = matcher.group(1)
                    val symbolNumber = matcher.group(2)
                    if (lineNumber != null && symbolNumber != null) {
                        try {
                            addMessage(GradleMessage(severity, message, path, lineNumber.toInt(), symbolNumber.toInt()), messages)
                            return true
                        }
                        catch (e: NumberFormatException) {
                            // ignore
                        }

                    }
                }

                addMessage(GradleMessage(severity, message), messages)
                return true
            }
            else {
                addMessage(GradleMessage(severity, lineWoSeverity.amendNextLinesIfNeeded(reader)), messages)
                return true
            }
        }

        return false
    }
}

private val COLON = ":"
private val POSITION_PATTERN = Pattern.compile("\\(([0-9]*), ([0-9]*)\\)")

private fun String.amendNextLinesIfNeeded(reader: OutputLineReader): String {
    var nextLine = reader.readLine()

    val builder = StringBuilder(this)
    while (nextLine != null && nextLine!!.isNextMessage().not()) {
        builder.append("\n").append(nextLine)
        if (!reader.hasNextLine()) break

        nextLine = reader.readLine()
    }

    if (nextLine != null) reader.pushBack(nextLine!!)

    return builder.toString()
}

private fun String.isNextMessage(): Boolean {
    val colonIndex1 = indexOf(COLON)
    return (colonIndex1 >= 0 && substring(0, colonIndex1).parseSeverity() != null) || StringUtil.containsIgnoreCase(this, "FAILURE")
}

private fun String.parseSeverity(): GradleMessage.Kind? {
    return when (this.trim()) {
        "e" -> GradleMessage.Kind.ERROR
        "w" -> GradleMessage.Kind.WARNING
        "i" -> GradleMessage.Kind.INFO
        "v" -> GradleMessage.Kind.SIMPLE
        else -> null
    }
}

private fun String.substringAfterAndTrim(index: Int) = substring(index + 1).trim()
private fun String.substringBeforeAndTrim(index: Int) = substring(0, index).trim()
private fun String.colon() = indexOf(COLON)
private fun Int.skipDriveOnWin(line: String): Int {
    return if (this == 1) line.indexOf(COLON, this + 1) else this
}

private fun addMessage(message: GradleMessage, messages: MutableList<GradleMessage>) {
    var duplicatesPrevious = false
    val messageCount = messages.size()
    if (messageCount > 0) {
        val lastMessage = messages.get(messageCount - 1)
        duplicatesPrevious = lastMessage == message
    }
    if (!duplicatesPrevious) {
        messages.add(message)
    }
}

