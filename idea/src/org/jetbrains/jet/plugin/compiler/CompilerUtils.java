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

package org.jetbrains.jet.plugin.compiler;

import com.google.common.collect.ImmutableMap;
import com.intellij.compiler.impl.javaCompiler.OutputItemImpl;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.intellij.openapi.compiler.CompilerMessageCategory.*;

/**
 * @author Pavel Talanov
 */
public final class CompilerUtils {
    private static SoftReference<URLClassLoader> ourClassLoaderRef = new SoftReference<URLClassLoader>(null);
    static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.compiler.CompilerUtils");

    private CompilerUtils() {
    }

    public static List<File> kompilerClasspath(File kotlinHome, CompileContext context) {
        File libs = new File(kotlinHome, "lib");

        if (!libs.exists() || libs.isFile()) {
            context.addMessage(ERROR, "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed", "", -1,
                               -1);
            return Collections.emptyList();
        }

        ArrayList<File> answer = new ArrayList<File>();
        answer.add(new File(libs, "kotlin-compiler.jar"));
        return answer;
    }

    public static URLClassLoader getOrCreateClassLoader(File kotlinHome, CompileContext context) {
        URLClassLoader answer = ourClassLoaderRef.get();
        if (answer == null) {
            answer = createClassloader(kotlinHome, context);
            ourClassLoaderRef = new SoftReference<URLClassLoader>(answer);
        }
        return answer;
    }

    private static URLClassLoader createClassloader(File kotlinHome, CompileContext context) {
        List<File> jars = kompilerClasspath(kotlinHome, context);
        URL[] urls = new URL[jars.size()];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = jars.get(i).toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e); // Checked exceptions are great! I love them, and I love brilliant library designers too!
            }
        }
        return new URLClassLoader(urls, null);
    }

    static void handleProcessTermination(int exitCode, CompileContext compileContext) {
        if (exitCode != 0 && exitCode != 1) {
            compileContext.addMessage(ERROR, "Compiler terminated with exit code: " + exitCode, "", -1, -1);
        }
    }

    public static void parseCompilerMessagesFromReader(CompileContext compileContext, final Reader reader, OutputItemsCollector collector) {
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
            parser.parse(new InputSource(wrappingReader), new CompilerOutputSAXHandler(compileContext, collector));
        }
        catch (Throwable e) {

            // Load all the text into the stringBuilder
            try {
                // This will not close the reader (see the wrapper above)
                FileUtil.loadTextAndClose(wrappingReader);
            }
            catch (IOException ioException) {
                LOG.error(ioException);
            }
            String message = stringBuilder.toString();
            LOG.error(message);
            LOG.error(e);
            compileContext.addMessage(ERROR, message, null, -1, -1);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public static int getReturnCodeFromObject(Object rc) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if ("org.jetbrains.jet.cli.common.ExitCode".equals(rc.getClass().getCanonicalName())) {
            return (Integer)rc.getClass().getMethod("getCode").invoke(rc);
        }
        else {
            throw new IllegalStateException("Unexpected return: " + rc);
        }
    }

    public static Object invokeExecMethod(CompilerEnvironment environment,
            PrintStream out,
            CompileContext context, String[] arguments, String name) throws Exception {
        URLClassLoader loader = getOrCreateClassLoader(environment.getKotlinHome(), context);
        Class<?> kompiler = Class.forName(name, true, loader);
        Method exec = kompiler.getMethod("exec", PrintStream.class, String[].class);
        return exec.invoke(kompiler.newInstance(), out, arguments);
    }

    private static class CompilerOutputSAXHandler extends DefaultHandler {
        private static final Map<String, CompilerMessageCategory> CATEGORIES = ImmutableMap.<String, CompilerMessageCategory>builder()
                .put("error", CompilerMessageCategory.ERROR)
                .put("warning", CompilerMessageCategory.WARNING)
                .put("logging", CompilerMessageCategory.STATISTICS)
                .put("exception", CompilerMessageCategory.ERROR)
                .put("info", CompilerMessageCategory.INFORMATION)
                .put("messages", CompilerMessageCategory.INFORMATION) // Root XML element
                .build();

        private final CompileContext compileContext;
        private final OutputItemsCollector collector;

        private final StringBuilder message = new StringBuilder();
        private Stack<String> tags = new Stack<String>();
        private String path;
        private int line;
        private int column;

        public CompilerOutputSAXHandler(CompileContext compileContext, OutputItemsCollector collector) {
            this.compileContext = compileContext;
            this.collector = collector;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            tags.push(qName);

            message.setLength(0);

            String rawPath = attributes.getValue("path");
            path = rawPath == null ? null : "file://" + rawPath;
            line = safeParseInt(attributes.getValue("line"), -1);
            column = safeParseInt(attributes.getValue("column"), -1);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (tags.size() == 1) {
                // We're directly inside the root tag: <MESSAGES>
                String message = new String(ch, start, length);
                if (!message.trim().isEmpty()) {
                    compileContext.addMessage(ERROR, "Unhandled compiler output: " + message, null, -1, -1);
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
            CompilerMessageCategory category = CATEGORIES.get(qNameLowerCase);
            if (category == null) {
                compileContext.addMessage(ERROR, "Unknown compiler message tag: " + qName, null, -1, -1);
                category = INFORMATION;
            }
            String text = message.toString();

            if ("exception".equals(qNameLowerCase)) {
                LOG.error(text);
            }

            if (category == STATISTICS) {
                compileContext.getProgressIndicator().setText(text);
                collector.learn(text);
            }
            else {
                compileContext.addMessage(category, text, path, line, column);
            }
            tags.pop();
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

    public static class OutputItemsCollectorImpl implements OutputItemsCollector {
        private static final String FOR_SOURCE_PREFIX = "For source: ";
        private static final String EMITTING_PREFIX = "Emitting: ";
        private final String outputPath;
        private VirtualFile currentSource;
        private List<TranslatingCompiler.OutputItem> answer = new ArrayList<TranslatingCompiler.OutputItem>();
        private List<VirtualFile> sources = new ArrayList<VirtualFile>();

        public OutputItemsCollectorImpl(String outputPath) {
            this.outputPath = outputPath;
        }

        @Override
        public void learn(String message) {
            message = message.trim();
            if (message.startsWith(FOR_SOURCE_PREFIX)) {
                currentSource = LocalFileSystem.getInstance().findFileByPath(message.substring(FOR_SOURCE_PREFIX.length()));
                if (currentSource != null) {
                    sources.add(currentSource);
                }
            }
            else if (message.startsWith(EMITTING_PREFIX)) {
                if (currentSource != null) {
                    OutputItemImpl item = new OutputItemImpl(outputPath + "/" + message.substring(EMITTING_PREFIX.length()), currentSource);
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(item.getOutputPath()));
                    answer.add(item);
                }
            }
        }

        public List<TranslatingCompiler.OutputItem> getOutputs() {
            return answer;
        }

        public List<VirtualFile> getSources() {
            return sources;
        }
    }

    public static void outputCompilerMessagesAndHandleExitCode(@NotNull CompileContext context,
            @NotNull OutputItemsCollector collector,
            @NotNull Function1<PrintStream, Integer> compilerRun) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);

        int exitCode = compilerRun.invoke(out);

        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        parseCompilerMessagesFromReader(context, reader, collector);
        handleProcessTermination(exitCode, context);
    }
}

