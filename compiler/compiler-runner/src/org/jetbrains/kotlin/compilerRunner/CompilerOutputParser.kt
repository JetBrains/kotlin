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

package org.jetbrains.kotlin.compilerRunner

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.io.IOException
import java.io.Reader

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.Companion.NO_LOCATION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil.reportException

object CompilerOutputParser {
    fun parseCompilerMessagesFromReader(messageCollector: MessageCollector, reader: Reader, collector: OutputItemsCollector) {
        // Sometimes the compiler doesn't output valid XML.
        // Example: error in command line arguments passed to the compiler.
        // The compiler will print the usage and the SAX parser will break.
        // In this case, we want to read everything from this stream and report it as an IDE error.
        val stringBuilder = StringBuilder()
        val wrappingReader = object : Reader() {
            @Throws(IOException::class)
            override fun read(cbuf: CharArray, off: Int, len: Int): Int {
                val read = reader.read(cbuf, off, len)
                stringBuilder.append(cbuf, off, len)
                return read
            }

            @Throws(IOException::class)
            override fun close() {
                // Do nothing:
                // If the SAX parser sees a syntax error, it throws an exception
                // and calls close() on the reader.
                // We prevent hte reader from being closed here, and close it later,
                // when all the text is read from it
            }
        }
        try {
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            parser.parse(InputSource(wrappingReader), CompilerOutputSAXHandler(messageCollector, collector))
        }
        catch (e: Throwable) {
            // Load all the text into the stringBuilder
            try {
                // This will not close the reader (see the wrapper above)
                FileUtil.loadTextAndClose(wrappingReader)
            }
            catch (ioException: IOException) {
                reportException(messageCollector, ioException)
            }

            val message = stringBuilder.toString()
            reportException(messageCollector, IllegalStateException(message, e))
            messageCollector.report(ERROR, message, NO_LOCATION)
        }
        finally {
            try {
                reader.close()
            }
            catch (e: IOException) {
                reportException(messageCollector, e)
            }

        }
    }

    private class CompilerOutputSAXHandler(private val messageCollector: MessageCollector, private val collector: OutputItemsCollector) : DefaultHandler() {

        private val message = StringBuilder()
        private val tags = Stack<String>()
        private var path: String? = null
        private var line: Int = 0
        private var column: Int = 0

        @Throws(SAXException::class)
        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            tags.push(qName)

            message.setLength(0)

            path = attributes.getValue("path")
            line = safeParseInt(attributes.getValue("line"), -1)
            column = safeParseInt(attributes.getValue("column"), -1)
        }

        @Throws(SAXException::class)
        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (tags.size == 1) {
                // We're directly inside the root tag: <MESSAGES>
                val message = String(ch!!, start, length)
                if (!message.trim { it <= ' ' }.isEmpty()) {
                    messageCollector.report(ERROR, "Unhandled compiler output: " + message, NO_LOCATION)
                }
            }
            else {
                message.append(ch, start, length)
            }
        }

        @Throws(SAXException::class)
        override fun endElement(uri: String?, localName: String, qName: String) {
            if (tags.size == 1) {
                // We're directly inside the root tag: <MESSAGES>
                return
            }
            val qNameLowerCase = qName.toLowerCase()
            var category: CompilerMessageSeverity? = CATEGORIES[qNameLowerCase]
            if (category == null) {
                messageCollector.report(ERROR, "Unknown compiler message tag: " + qName, NO_LOCATION)
                category = INFO
            }
            val text = message.toString()

            if (category == OUTPUT) {
                reportToCollector(text)
            }
            else {
                messageCollector.report(category, text, CompilerMessageLocation.create(path, line, column, null))
            }
            tags.pop()
        }

        private fun reportToCollector(text: String) {
            val output = OutputMessageUtil.parseOutputMessage(text)
            if (output != null) {
                collector.add(output.sourceFiles, output.outputFile)
            }
        }

        companion object {
            private val CATEGORIES = ContainerUtil.ImmutableMapBuilder<String, CompilerMessageSeverity>()
                    .put("error", ERROR)
                    .put("warning", WARNING)
                    .put("logging", LOGGING)
                    .put("output", OUTPUT)
                    .put("exception", EXCEPTION)
                    .put("info", INFO)
                    .put("messages", INFO) // Root XML element
                    .build()

            private fun safeParseInt(value: String?, defaultValue: Int): Int {
                if (value == null) {
                    return defaultValue
                }
                try {
                    return Integer.parseInt(value.trim { it <= ' ' })
                }
                catch (e: NumberFormatException) {
                    return defaultValue
                }

            }
        }
    }
}
