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

package org.jetbrains.kotlin.android

import com.android.ide.common.blame.Message
import com.android.ide.common.blame.SourceFilePosition
import com.android.ide.common.blame.SourcePosition
import com.android.ide.common.blame.parser.util.OutputLineReader
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import java.io.File
import java.lang.IllegalStateException
import java.util.regex.Pattern

fun parse(line: String, reader: OutputLineReader, messages: MutableList<Message>): Boolean {
    val colonIndex1 = line.colon()
    val severity = if (colonIndex1 >= 0) line.substringBeforeAndTrim(colonIndex1) else return false
    if (!severity.startsWithSeverityPrefix()) return false

    val lineWoSeverity = line.substringAfterAndTrim(colonIndex1)
    val colonIndex2 = lineWoSeverity.colon().skipDriveOnWin(lineWoSeverity)
    if (colonIndex2 >= 0) {
        val path = lineWoSeverity.substringBeforeAndTrim(colonIndex2)
        val file = File(path)

        val fileExtension = file.extension.toLowerCase()
        if (!file.isFile || (fileExtension != "kt" && fileExtension != "java")) {
            return addMessage(createMessage(getMessageKind(severity), lineWoSeverity.amendNextLinesIfNeeded(reader)), messages)
        }

        val lineWoPath = lineWoSeverity.substringAfterAndTrim(colonIndex2)
        val colonIndex3 = lineWoPath.colon()
        if (colonIndex3 >= 0) {
            val position = lineWoPath.substringBeforeAndTrim(colonIndex3)

            val matcher = KOTLIN_POSITION_PATTERN.matcher(position).takeIf { it.matches() } ?: JAVAC_POSITION_PATTERN.matcher(position)
            val message = lineWoPath.substringAfterAndTrim(colonIndex3).amendNextLinesIfNeeded(reader)

            if (matcher.matches()) {
                val lineNumber = matcher.group(1)
                val symbolNumber = if (matcher.groupCount() >= 2) matcher.group(2) else "1"
                if (lineNumber != null) {
                    val symbolNumberText = symbolNumber.toInt()
                    return addMessage(createMessageWithLocation(
                            getMessageKind(severity), message, path, lineNumber.toInt(), symbolNumberText, symbolNumberText), messages)
                }
            }

            return addMessage(createMessage(getMessageKind(severity), message), messages)
        }
        else {
            return addMessage(createMessage(getMessageKind(severity), lineWoSeverity.amendNextLinesIfNeeded(reader)), messages)
        }
    }

    return false
}

private val COLON = ":"
private val KOTLIN_POSITION_PATTERN = Pattern.compile("\\(([0-9]*), ([0-9]*)\\)")
private val JAVAC_POSITION_PATTERN = Pattern.compile("([0-9]+)")

private fun String.amendNextLinesIfNeeded(reader: OutputLineReader): String {
    var nextLine = reader.readLine()

    val builder = StringBuilder(this)
    while (nextLine != null && nextLine.isNextMessage().not()) {
        builder.append("\n").append(nextLine)
        if (!reader.hasNextLine()) break

        nextLine = reader.readLine()
    }

    if (nextLine != null) {
        // This code is needed for compatibility with AS 2.0 and IDEA 15.0, because of difference in android plugins
        val positionField = try {
            reader::class.java.getDeclaredField("myPosition")
        }
        catch(e: Throwable) {
            null
        }
        if (positionField != null) {
            positionField.isAccessible = true
            positionField.setInt(reader, positionField.getInt(reader) - 1)
        }
    }

    return builder.toString()
}

private fun String.isNextMessage(): Boolean {
    val colonIndex1 = indexOf(COLON)
    return colonIndex1 == 0
           || (colonIndex1 >= 0 && substring(0, colonIndex1).startsWithSeverityPrefix()) // Next Kotlin message
           || StringUtil.containsIgnoreCase(this, "FAILURE")
           || StringUtil.containsIgnoreCase(this, "FAILED")
}

private fun String.startsWithSeverityPrefix() = getMessageKind(this) != Message.Kind.UNKNOWN

private fun getMessageKind(kind: String) = when (kind) {
    "e" -> Message.Kind.ERROR
    "w" -> Message.Kind.WARNING
    "i" -> Message.Kind.INFO
    "v" -> Message.Kind.SIMPLE
    else -> Message.Kind.UNKNOWN
}

private fun String.substringAfterAndTrim(index: Int) = substring(index + 1).trim()
private fun String.substringBeforeAndTrim(index: Int) = substring(0, index).trim()
private fun String.colon() = indexOf(COLON)
private fun Int.skipDriveOnWin(line: String): Int {
    return if (this == 1) line.indexOf(COLON, this + 1) else this
}

private val KAPT_ERROR_WHILE_ANNOTATION_PROCESSING_MARKER_TEXT =
        KaptError::class.java.canonicalName + ": " + KaptError.Kind.ERROR_RAISED.message

private fun isKaptErrorWhileAnnotationProcessing(message: Message): Boolean {
    if (message.kind != Message.Kind.ERROR) return false
    if (message.sourceFilePositions.singleOrNull() != SourceFilePosition.UNKNOWN) return false

    val messageText = message.text
    return messageText.startsWith(IllegalStateException::class.java.name)
           && messageText.contains(KAPT_ERROR_WHILE_ANNOTATION_PROCESSING_MARKER_TEXT)
}

private fun addMessage(message: Message, messages: MutableList<Message>): Boolean {
    // Ignore KaptError.ERROR_RAISED message from kapt. We already processed all errors from annotation processing
    if (isKaptErrorWhileAnnotationProcessing(message)) return true

    var duplicatesPrevious = false
    val messageCount = messages.size
    if (messageCount > 0) {
        val lastMessage = messages[messageCount - 1]
        duplicatesPrevious = lastMessage == message
    }
    if (!duplicatesPrevious) {
        messages.add(message)
    }
    return true
}

private fun createMessage(messageKind: Message.Kind, text: String): Message {
    return Message(messageKind, text.trim(), text, Optional.absent<String>(), ImmutableList.of())
}

private fun createMessageWithLocation(
        messageKind: Message.Kind,
        text: String,
        file: String,
        lineNumber: Int,
        columnIndex: Int,
        offset: Int
): Message {
    val sourcePosition = SourcePosition(lineNumber - 1, columnIndex - 1, offset)
    val sourceFilePosition = SourceFilePosition(File(file), sourcePosition)
    return Message(messageKind, text.trim(), sourceFilePosition)
}
