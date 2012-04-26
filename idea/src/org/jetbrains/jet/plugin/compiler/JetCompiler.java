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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.compiler.impl.javaCompiler.OutputItemImpl;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.project.JsModuleDetector;
import org.jetbrains.jet.utils.PathUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.intellij.openapi.compiler.CompilerMessageCategory.*;

/**
 * @author yole
 */
public class JetCompiler implements TranslatingCompiler {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.compiler.JetCompiler");

    private static final boolean RUN_OUT_OF_PROCESS = false;

    @Override
    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        if (!(virtualFile.getFileType() instanceof JetFileType)) {
            return false;
        }
        Project project = compileContext.getProject();
        if (project == null || JsModuleDetector.isJsProject(project)) {
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Jet Language Compiler";
    }

    @Override
    public boolean validateConfiguration(CompileScope compileScope) {
        return true;
    }

    @Override
    public void compile(final CompileContext compileContext, Chunk<Module> moduleChunk, final VirtualFile[] virtualFiles, OutputSink outputSink) {
        if (virtualFiles.length == 0) return;

        List<VirtualFile> productionFiles = new ArrayList<VirtualFile>();
        List<VirtualFile> testFiles = new ArrayList<VirtualFile>();
        for (VirtualFile file : virtualFiles) {
            final boolean inTests = ((CompileContextEx)compileContext).isInTestSourceContent(file);
            if (inTests) {
                testFiles.add(file);
            }
            else {
                productionFiles.add(file);
            }
        }

        final Module module = compileContext.getModuleByFile(virtualFiles[0]);

        doCompile(compileContext, moduleChunk, productionFiles, module, outputSink, false);
        doCompile(compileContext, moduleChunk, testFiles, module, outputSink, true);
    }

    private void doCompile(CompileContext compileContext, Chunk<Module> moduleChunk, List<VirtualFile> files, Module module, OutputSink outputSink, boolean tests) {
        if (files.isEmpty()) return;

        VirtualFile mainOutput = compileContext.getModuleOutputDirectory(module);
        final VirtualFile outputDir = tests ? compileContext.getModuleOutputDirectoryForTests(module) : mainOutput;
        if (outputDir == null) {
            compileContext.addMessage(ERROR, "[Internal Error] No output directory", "", -1, -1);
            return;
        }

        File kotlinHome = PathUtil.getDefaultCompilerPath();
        if (kotlinHome == null) {
            compileContext.addMessage(ERROR, "Cannot find kotlinc home. Make sure plugin is properly installed", "", -1, -1);
            return;
        }

        ModuleChunk chunk = new ModuleChunk((CompileContextEx) compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());
        String moduleName = moduleChunk.getNodes().iterator().next().getName();

        // Filter the output we are writing to
        Set<VirtualFile> outputDirectoriesToFilter = Sets.newHashSet(compileContext.getModuleOutputDirectoryForTests(module));
        if (!tests) {
            outputDirectoriesToFilter.add(compileContext.getModuleOutputDirectory(module));
        }
        CharSequence script = generateModuleScript(moduleName, chunk, files, tests, mainOutput, outputDirectoriesToFilter);

        File scriptFile = new File(path(outputDir), "script.kts");
        try {
            FileUtil.writeToFile(scriptFile, script.toString());
        } catch (IOException e) {
            compileContext.addMessage(ERROR, "[Internal Error] Cannot write script to " + scriptFile.getAbsolutePath(), "", -1, -1);
            return;
        }

        OutputItemsCollectorImpl collector = new OutputItemsCollectorImpl(outputDir.getPath());

        if (RUN_OUT_OF_PROCESS) {
            runOutOfProcess(compileContext, collector, outputDir, kotlinHome, scriptFile);
        }
        else {
            runInProcess(compileContext, collector, outputDir, kotlinHome, scriptFile);
        }

        outputSink.add(outputDir.getPath(), collector.getOutputs(), collector.getSources().toArray(VirtualFile.EMPTY_ARRAY));
//        System.out.println("Generated module script:\n" + script.toString());
//        compileContext.addMessage(INFORMATION, "Generated module script:\n" + script.toString(), "file://" + path(mainOutput), 0, 1);
    }

    private static CharSequence generateModuleScript(String moduleName, ModuleChunk chunk, List<VirtualFile> files, boolean tests, VirtualFile mainOutput, Set<VirtualFile> directoriesToFilterOut) {
        StringBuilder script = new StringBuilder();

        if (tests) {
            script.append("// Module script for tests\n");
        }
        else {
            script.append("// Module script for production\n");
        }

        script.append("import kotlin.modules.*\n");
        script.append("fun project() {\n");
        script.append("    module(\"" + moduleName + "\") {\n");

        for (VirtualFile sourceFile : files) {
            script.append("        sources += \"" + path(sourceFile) + "\"\n");
        }

        // TODO: have a bootclasspath in script API
        script.append("        // Boot classpath\n");
        for (VirtualFile root : chunk.getCompilationBootClasspathFiles()) {
            script.append("        classpath += \"" + path(root) + "\"\n");
        }

        script.append("        // Compilation classpath\n");
        for (VirtualFile root : chunk.getCompilationClasspathFiles()) {
            String path = path(root);
            if (directoriesToFilterOut.contains(root)) {
                // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                script.append("        // Output directory, commented out\n");
                script.append("        // ");
            }
            script.append("        classpath += \"" + path + "\"\n");
        }

        // This is for java files in same roots
        script.append("        // Java classpath (for Java sources)\n");
        for (VirtualFile root : chunk.getSourceRoots()) {
            script.append("        classpath += \"" + path(root) + "\"\n");
        }

        script.append("        // Main output\n");
        if (tests && mainOutput != null) {
            script.append("        classpath += \"" + path(mainOutput) + "\"\n");
        }

        script.append("    }\n");
        script.append("}\n");
        return script;
    }

    private static List<File> getJarsInDirectory(File dir) {
        List<File> r = Lists.newArrayList();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File jar : files) {
                if (jar.isFile() && jar.getName().endsWith(".jar")) {
                    r.add(jar);
                }
            }
        }

        return r;
    }

    private static List<File> kompilerClasspath(File kotlinHome, CompileContext context) {
        File libs = new File(kotlinHome, "lib");

        if (!libs.exists() || libs.isFile()) {
            context.addMessage(ERROR, "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed", "", -1, -1);
            return Collections.emptyList();
        }


        ArrayList<File> answer = new ArrayList<File>();
        answer.addAll(getJarsInDirectory(libs));
        answer.addAll(getJarsInDirectory(new File(libs, "guice"))); // TODO: flatten at artifact build
        return answer;
    }

    private void runInProcess(final CompileContext compileContext, OutputItemsCollector collector, VirtualFile outputDir, File kotlinHome, File scriptFile) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);

        int exitCode = execInProcess(kotlinHome, outputDir, scriptFile, out, compileContext);

        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        parseCompilerMessagesFromReader(compileContext, reader, collector);
        handleProcessTermination(exitCode, compileContext);
    }

    private static void parseCompilerMessagesFromReader(CompileContext compileContext, final Reader reader, OutputItemsCollector collector) {
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
                // If the SAX parser sees a syntax error, it throws an expcetion
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

    private static void handleProcessTermination(int exitCode, CompileContext compileContext) {
        if (exitCode != 0 && exitCode != 1) {
            compileContext.addMessage(ERROR, "Compiler terminated with exit code: " + exitCode, "", -1, -1);
        }
    }

    private static int execInProcess(File kotlinHome, VirtualFile outputDir, File scriptFile, PrintStream out, CompileContext context) {
        URLClassLoader loader = getOrCreateClassLoader(kotlinHome, context);
        try {
            String compilerClassName = "org.jetbrains.jet.cli.jvm.K2JVMCompiler";
            Class<?> kompiler = Class.forName(compilerClassName, true, loader);
            Method exec = kompiler.getMethod("exec", PrintStream.class, String[].class);

            String[] arguments = commandLineArguments(outputDir, scriptFile);

            context.addMessage(INFORMATION, "Using kotlinHome=" + kotlinHome, "", -1, -1);
            context.addMessage(INFORMATION, "Invoking in-process compiler " + compilerClassName + " with arguments " + Arrays.asList(arguments), "", -1, -1);
            
            Object rc = exec.invoke(kompiler.newInstance(), out, arguments);
            // exec() returns a K2JVMCompiler.ExitCode object, that class is not accessible here,
            // so we take it's contents through reflection
            if ("org.jetbrains.jet.cli.common.ExitCode".equals(rc.getClass().getCanonicalName())) {
                return (Integer) rc.getClass().getMethod("getCode").invoke(rc);
            }
            else {
                throw new IllegalStateException("Unexpected return: " + rc);
            }
        } catch (Throwable e) {
            LOG.error(e);
            return -1;
        }
    }

    private static String[] commandLineArguments(VirtualFile outputDir, File scriptFile) {
        return new String[]{ "-module", scriptFile.getAbsolutePath(), "-output", path(outputDir), "-tags", "-verbose", "-version", "-mode", "stdlib" };
    }

    private static SoftReference<URLClassLoader> ourClassLoaderRef = new SoftReference<URLClassLoader>(null);

    private static URLClassLoader getOrCreateClassLoader(File kotlinHome, CompileContext context) {
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
            } catch (MalformedURLException e) {
                throw new RuntimeException(e); // Checked exceptions are great! I love them, and I love brilliant library designers too!
            }
        }

        return new URLClassLoader(urls, null);
    }

    private static void runOutOfProcess(final CompileContext compileContext, final OutputItemsCollector collector, VirtualFile outputDir, File kotlinHome, File scriptFile) {
        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.jvm.K2JVMCompiler");

        for (String arg : commandLineArguments(outputDir, scriptFile)) {
            params.getProgramParametersList().add(arg);
        }

        for (File jar : kompilerClasspath(kotlinHome, compileContext)) {
            params.getClassPath().add(jar);
        }

        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
//        params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

        Sdk sdk = params.getJdk();

        final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(
                ((JavaSdkType) sdk.getSdkType()).getVMExecutablePath(sdk), params, false);
        
        compileContext.addMessage(INFORMATION, "Invoking out-of-process compiler with arguments: " + commandLine, "", -1, -1);
        
        try {
            final Process process = commandLine.createProcess();

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    parseCompilerMessagesFromReader(compileContext, new InputStreamReader(process.getInputStream()), collector);
                }
            });

            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtil.loadBytes(process.getErrorStream());
                    }
                    catch (IOException e) {
                        // Don't care
                    }
                }
            });

            int exitCode = process.waitFor();
            handleProcessTermination(exitCode, compileContext);
        }
        catch (Exception e) {
            compileContext.addMessage(ERROR, "[Internal Error] " + e.getLocalizedMessage(), "", -1, -1);
            return;
        }
    }

    public static class OutputItemsCollectorImpl implements OutputItemsCollector {
        private static final String FOR_SOURCE_PREFIX = "For source: ";
        private static final String EMITTING_PREFIX = "Emitting: ";
        private final String outputPath;
        private VirtualFile currentSource;
        private List<OutputItem> answer = new ArrayList<OutputItem>();
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

        public List<OutputItem> getOutputs() {
            return answer;
        }

        public List<VirtualFile> getSources() {
            return sources;
        }
    }

    private static String path(VirtualFile root) {
        String path = root.getPath();
        if (path.endsWith("!/")) {
            return path.substring(0, path.length() - 2);
        }

        return path;
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
}
