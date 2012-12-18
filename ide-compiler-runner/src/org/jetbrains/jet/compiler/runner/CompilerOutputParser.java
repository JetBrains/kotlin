/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler.runner;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.*;

public class CompilerOutputParser {
    public static void parseCompilerMessagesFromReader(MessageCollector messageCollector, final Reader reader, OutputItemsCollector collector) {
        // Sometimes the compiler can't output valid XML
        // Example: error in command line arguments passed to the compiler
        // having no -tags key (arguments are not parsed), the compiler doesn't know
        // if it should put any tags in the output, so it will simply print the usage
        // and the SAX parser will break.
        // In this case, we want to read everything from this stream
        // and report it as an IDE error.
        final StringBuilder stringBuilder = new StringBuilder();
        //noinspection IOResourceOpenedButNotSafelyClosed
        Reader wrappingReader = new Reader() {

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int read = reader.read(cbuf, off, len);
                stringBuilder.append(cbuf, off, len);
                return read;
            }

            @Override
            public void close() throws IOException {
                // Do nothing:
                // If the SAX parser sees a syntax error, it throws an exception
                // and calls close() on the reader.
                // We prevent hte reader from being closed here, and close it later,
                // when all the text is read from it
            }
        };
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(wrappingReader), new CompilerOutputSAXHandler(messageCollector, collector));
        }
        catch (Throwable e) {

            // Load all the text into the stringBuilder
            try {
                // This will not close the reader (see the wrapper above)
                FileUtil.loadTextAndClose(wrappingReader);
            }
            catch (IOException ioException) {
                reportException(messageCollector, ioException);
            }
            String message = stringBuilder.toString();
            reportException(messageCollector, new IllegalStateException(message, e));
            messageCollector.report(ERROR, message, NO_LOCATION);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                reportException(messageCollector, e);
            }
        }
    }

    public static void reportException(@NotNull MessageCollector messageCollector, @NotNull Throwable e) {
        messageCollector.report(EXCEPTION, MessageRenderer.PLAIN.renderException(e), NO_LOCATION);
    }

    private static class CompilerOutputSAXHandler extends DefaultHandler {
        private static final Map<String, CompilerMessageSeverity> CATEGORIES = new ContainerUtil.ImmutableMapBuilder<String, CompilerMessageSeverity>()
                .put("error", ERROR)
                .put("warning", WARNING)
                .put("logging", LOGGING)
                .put("output", OUTPUT)
                .put("exception", EXCEPTION)
                .put("info", INFO)
                .put("messages", INFO) // Root XML element
                .build();

        private final MessageCollector messageCollector;
        private final OutputItemsCollector collector;

        private final StringBuilder message = new StringBuilder();
        private Stack<String> tags = new Stack<String>();
        private String path;
        private int line;
        private int column;

        public CompilerOutputSAXHandler(MessageCollector messageCollector, OutputItemsCollector collector) {
            this.messageCollector = messageCollector;
            this.collector = collector;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            tags.push(qName);

            message.setLength(0);

            String rawPath = attributes.getValue("path");
            path = rawPath == null ? null : rawPath;
            line = safeParseInt(attributes.getValue("line"), -1);
            column = safeParseInt(attributes.getValue("column"), -1);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (tags.size() == 1) {
                // We're directly inside the root tag: <MESSAGES>
                String message = new String(ch, start, length);
                if (!message.trim().isEmpty()) {
                    messageCollector.report(ERROR, "Unhandled compiler output: " + message, NO_LOCATION);
                }
            }
            else {
                message.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (tags.size() == 1) {
                // We're directly inside the root tag: <MESSAGES>
                return;
            }
            String qNameLowerCase = qName.toLowerCase();
            CompilerMessageSeverity category = CATEGORIES.get(qNameLowerCase);
            if (category == null) {
                messageCollector.report(ERROR, "Unknown compiler message tag: " + qName, NO_LOCATION);
                category = INFO;
            }
            String text = message.toString();

            if (category == OUTPUT) {
                reportToCollector(text);
            }
            else {
                messageCollector.report(category, text, CompilerMessageLocation.create(path, line, column));
            }
            tags.pop();
        }

        private void reportToCollector(String text) {
            OutputMessageUtil.Output output = OutputMessageUtil.parseOutputMessage(text);
            if (output != null) {
                collector.add(output.sourceFiles, output.outputFile);
            }
        }

        private static int safeParseInt(@Nullable String value, int defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value.trim());
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }
}
