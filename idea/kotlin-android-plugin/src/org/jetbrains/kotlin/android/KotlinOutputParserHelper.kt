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

import com.android.ide.common.blame.parser.util.OutputLineReader
import com.android.utils.ILogger
import com.google.common.collect.ImmutableList
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.lang.reflect.Constructor
import java.util.*
import java.util.regex.Pattern
import java.lang.reflect.Array as RArray

fun parse(line: String, reader: OutputLineReader, messages: MutableList<Any>, logger: ILogger): Boolean {
    val colonIndex1 = line.colon()
    val severity = if (colonIndex1 >= 0) line.substringBeforeAndTrim(colonIndex1) else return false
    if (!severity.startsWithSeverityPrefix()) return false

    val lineWoSeverity = line.substringAfterAndTrim(colonIndex1)
    val colonIndex2 = lineWoSeverity.colon().skipDriveOnWin(lineWoSeverity)
    if (colonIndex2 >= 0) {
        val path = lineWoSeverity.substringBeforeAndTrim(colonIndex2)
        val file = File(path)
        if (!file.isFile && FileUtilRt.getExtension(file.name) != "kt") {
            return addMessage(KotlinOutputParserHelper.createMessage(logger, severity, lineWoSeverity.amendNextLinesIfNeeded(reader)), messages)
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
                        return addMessage(KotlinOutputParserHelper.createMessage(logger, severity, message, path, lineNumber.toInt(), symbolNumber.toInt(), symbolNumber.toInt()), messages)
                    }
                    catch (e: NumberFormatException) {
                        // ignore
                    }

                }
            }

            return addMessage(KotlinOutputParserHelper.createMessage(logger, severity, message), messages)
        }
        else {
            return addMessage(KotlinOutputParserHelper.createMessage(logger, severity, lineWoSeverity.amendNextLinesIfNeeded(reader)), messages)
        }
    }

    return false
}

private val COLON = ":"
private val POSITION_PATTERN = Pattern.compile("\\(([0-9]*), ([0-9]*)\\)")

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
            reader.javaClass.getDeclaredField("myPosition")
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

private fun String.startsWithSeverityPrefix(): Boolean {
    return when (this.trim()) {
        "e", "w", "i", "v" -> true
        else -> false
    }
}

private fun String.substringAfterAndTrim(index: Int) = substring(index + 1).trim()
private fun String.substringBeforeAndTrim(index: Int) = substring(0, index).trim()
private fun String.colon() = indexOf(COLON)
private fun Int.skipDriveOnWin(line: String): Int {
    return if (this == 1) line.indexOf(COLON, this + 1) else this
}

private fun addMessage(message: Any?, messages: MutableList<Any>): Boolean {
    if (message == null) return false
    var duplicatesPrevious = false
    val messageCount = messages.size
    if (messageCount > 0) {
        val lastMessage = messages.get(messageCount - 1)
        duplicatesPrevious = lastMessage == message
    }
    if (!duplicatesPrevious) {
        messages.add(message)
    }
    return true
}

object KotlinOutputParserHelper {
    private val isNewAndroidPlugin: Boolean
    private val packagePrefix: String
    private val severityObjectMap: HashMap<String, Any> = HashMap()

    init {
        isNewAndroidPlugin = try {
            Class.forName("com.android.ide.common.blame.Message")
            true
        }
        catch (e: ClassNotFoundException) {
            false
        }

        packagePrefix = if (isNewAndroidPlugin) "com.android.ide.common.blame" else "com.android.ide.common.blame.output"

        loadSeverityEnums()
    }

    private val simpleMessageConstructor: Constructor<*> by lazy {
        if (!isNewAndroidPlugin) {
            val messageClass = Class.forName("$packagePrefix.GradleMessage")
            val messageKindClass = Class.forName("$packagePrefix.GradleMessage\$Kind")
            messageClass.getConstructor(messageKindClass, String::class.java)
        }
        else {
            val messageClass = Class.forName("$packagePrefix.Message")
            val messageKindClass = Class.forName("$packagePrefix.Message\$Kind")
            messageClass.getConstructor(
                    messageKindClass,
                    String::class.java,
                    String::class.java,
                    ImmutableList::class.java)
        }
    }

    private val complexMessageConstructor: Constructor<*> by lazy {
        if (!isNewAndroidPlugin) {
            val messageClass = Class.forName("$packagePrefix.GradleMessage")
            val messageKindClass = Class.forName("$packagePrefix.GradleMessage\$Kind")
            messageClass.getConstructor(
                    messageKindClass,
                    String::class.java, String::class.java,
                    Int::class.java, Int::class.java)
        }
        else {
            val messageClass = Class.forName("$packagePrefix.Message")
            val messageKindClass = Class.forName("$packagePrefix.Message\$Kind")
            val sourceFilePositionClass = Class.forName("$packagePrefix.SourceFilePosition")
            val sourceFilePositionArrayClass = Class.forName("[L$packagePrefix.SourceFilePosition;")
            messageClass.getConstructor(
                    messageKindClass,
                    String::class.java,
                    sourceFilePositionClass,
                    sourceFilePositionArrayClass)
        }
    }

    private val sourceFilePositionConstructor: Constructor<*> by lazy {
        assert(isNewAndroidPlugin) { "This property should be used only for New Android Plugin" }
        val sourcePositionClass = Class.forName("$packagePrefix.SourcePosition")
        val sourceFilePositionClass = Class.forName("$packagePrefix.SourceFilePosition")
        sourceFilePositionClass.getConstructor(File::class.java, sourcePositionClass)
    }

    private val sourcePositionConstructor: Constructor<*> by lazy {
        assert(isNewAndroidPlugin) { "This property should be used only for New Android Plugin" }
        val sourcePositionClass = Class.forName("$packagePrefix.SourcePosition")
        sourcePositionClass.getConstructor(Int::class.java, Int::class.java, Int::class.java)
    }

    private val sourcePositionVarargArg: Any by lazy {
        assert(isNewAndroidPlugin) { "This property should be used only for New Android Plugin" }
        val sourceFilePositionClass = Class.forName("$packagePrefix.SourceFilePosition")
        RArray.newInstance(sourceFilePositionClass, 0)
    }

    private fun loadSeverityEnums() {
        val messageKindClass = if (isNewAndroidPlugin)
                                    Class.forName("$packagePrefix.Message\$Kind")
                               else
                                    Class.forName("$packagePrefix.GradleMessage\$Kind")

        val messageKindConstants = messageKindClass.enumConstants as Array<Any>
        for (kind in messageKindConstants) {
            when(kind.toString()) {
                "ERROR" -> severityObjectMap.put("e", kind)
                "WARNING" -> severityObjectMap.put("w", kind)
                "INFO" -> severityObjectMap.put("i", kind)
                "SIMPLE" -> severityObjectMap.put("v", kind)
            }
        }
    }

    fun createMessage(
            logger: ILogger,
            severity: String,
            text: String,
            file: String? = null,
            lineNumber: Int? = null,
            columnIndex: Int? = null,
            offset: Int? = null
    ): Any? {
        try {
            val severityConst: Any = severityObjectMap[severity] ?: return null

            if (isNewAndroidPlugin) {
                return createNewMessage(severityConst, text.trim(), file, lineNumber, columnIndex, offset)
            }
            else {
                return createOldMessage(severityConst, text.trim(), file, lineNumber, columnIndex)
            }
        }
        catch(e: Throwable) {
            logger.error(e, "Exception from KotlinOutputParser")
        }

        return null
    }

    private fun createNewMessage(
            severityConst: Any,
            text: String,
            file: String?,
            lineNumber: Int?,
            columnIndex: Int?,
            offset: Int?
    ): Any? {
        if (file == null || lineNumber == null || columnIndex == null || offset == null) {
            return simpleMessageConstructor.newInstance(
                    severityConst,
                    text,
                    text,
                    ImmutableList.of<Any>())
        }
        else {
            val sourcePositionObj = sourcePositionConstructor.newInstance(lineNumber - 1, columnIndex - 1, offset)
            val sourceFilePositionObj = sourceFilePositionConstructor.newInstance(File(file), sourcePositionObj)

            return complexMessageConstructor.newInstance(
                    severityConst,
                    text,
                    sourceFilePositionObj,
                    sourcePositionVarargArg)
        }
    }

    private fun createOldMessage(
            severityConst: Any,
            text: String,
            file: String?,
            lineNumber: Int?,
            columnIndex: Int?
    ): Any? {
        if (file == null || lineNumber == null || columnIndex == null) {
            return simpleMessageConstructor.newInstance(severityConst, text)
        }
        else {
            return complexMessageConstructor.newInstance(
                    severityConst,
                    text, file,
                    lineNumber, columnIndex)
        }
    }
}
