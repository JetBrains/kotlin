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

import com.google.common.collect.Sets;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.execution.process.*;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.plugin.JetFileType;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.compiler.CompilerMessageCategory.*;

/**
 * @author yole
 */
public class JetCompiler implements TranslatingCompiler {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.compiler.JetCompiler");

    private static final boolean RUN_OUT_OF_PROCESS = false;

    @Override
    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        return virtualFile.getFileType() instanceof JetFileType;
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

        doCompile(compileContext, moduleChunk, productionFiles, module, false);
        doCompile(compileContext, moduleChunk, testFiles, module, true);
    }

    private void doCompile(CompileContext compileContext, Chunk<Module> moduleChunk, List<VirtualFile> files, Module module, boolean tests) {
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

        if (RUN_OUT_OF_PROCESS) {
            runOutOfProcess(compileContext, outputDir, kotlinHome, scriptFile);
        }
        else {
            runInProcess(compileContext, outputDir, kotlinHome, scriptFile);
        }

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

    private static List<File> kompilerClasspath(File kotlinHome, CompileContext context) {
        File libs = new File(kotlinHome, "lib");

        if (!libs.exists() || libs.isFile()) {
            context.addMessage(ERROR, "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed", "", -1, -1);
            return Collections.emptyList();
        }


        ArrayList<File> answer = new ArrayList<File>();
        File[] jars = libs.listFiles();
        if (jars != null) {
            for (File jar : jars) {
                if (jar.isFile() && jar.getName().endsWith(".jar")) {
                    answer.add(jar);
                }
            }
        }

        return answer;
    }

    private void runInProcess(CompileContext compileContext, VirtualFile outputDir, File kotlinHome, File scriptFile) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);

        int rc = execInProcess(kotlinHome, outputDir, scriptFile, out, compileContext);

        ProcessAdapter listener = createProcessListener(compileContext);

        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        while (true) {
            try {
                String line = reader.readLine();
                if (line == null) break;
                listener.onTextAvailable(new ProcessEvent(NullProcessHandler.INSTANCE, line + "\n"), ProcessOutputTypes.STDERR);
            } catch (IOException e) {
                // Can't be
                throw new IllegalStateException(e);
            }
        }

        ProcessEvent termintationEvent = new ProcessEvent(NullProcessHandler.INSTANCE, rc);
        listener.processWillTerminate(termintationEvent, false);
        listener.processTerminated(termintationEvent);
    }

    private static int execInProcess(File kotlinHome, VirtualFile outputDir, File scriptFile, PrintStream out, CompileContext context) {
        URLClassLoader loader = getOrCreateClassloader(kotlinHome, context);
        try {
            String compilerClassName = "org.jetbrains.jet.cli.KotlinCompiler";
            Class<?> kompiler = Class.forName(compilerClassName, true, loader);
            Method exec = kompiler.getDeclaredMethod("exec", PrintStream.class, String[].class);

            String[] arguments = { "-module", scriptFile.getAbsolutePath(), "-output", path(outputDir), "-tags" };

            context.addMessage(INFORMATION, "Using kotlinHome=" + kotlinHome, "", -1, -1);
            context.addMessage(INFORMATION, "Invoking in-process compiler " + compilerClassName + " with arguments " + Arrays.asList(arguments), "", -1, -1);
            
            Object rc = exec.invoke(kompiler.newInstance(), out, arguments);
            if (rc instanceof Integer) {
                return ((Integer) rc).intValue();
            }
            else {
                throw new IllegalStateException("Unexpected return: " + rc);
            }
        } catch (Throwable e) {
            LOG.error(e);
            return -1;
        }
    }

    private static SoftReference<URLClassLoader> ourClassloaderRef = new SoftReference<URLClassLoader>(null);

    private static URLClassLoader getOrCreateClassloader(File kotlinHome, CompileContext context) {
        URLClassLoader answer = ourClassloaderRef.get();
        if (answer == null) {
            answer = createClassloader(kotlinHome, context);
            ourClassloaderRef = new SoftReference<URLClassLoader>(answer);
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

    private static void runOutOfProcess(CompileContext compileContext, VirtualFile outputDir, File kotlinHome, File scriptFile) {
        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.KotlinCompiler");
        params.getProgramParametersList().add("-module", scriptFile.getAbsolutePath());
        params.getProgramParametersList().add("-output", path(outputDir));
        params.getProgramParametersList().add("-tags");

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
            final OSProcessHandler processHandler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString()) {
              @Override
              public Charset getCharset() {
                return commandLine.getCharset();
              }
            };

            ProcessAdapter processListener = createProcessListener(compileContext);
            processHandler.addProcessListener(processListener);

            processHandler.startNotify();
            processHandler.waitFor();
        } catch (Exception e) {
            compileContext.addMessage(ERROR, "[Internal Error] " + e.getLocalizedMessage(), "", -1, -1);
            return;
        }
    }

    private static ProcessAdapter createProcessListener(final CompileContext compileContext) {
        return new CompilerProcessListener(compileContext);
    }

    private static String path(VirtualFile root) {
        String path = root.getPath();
        if (path.endsWith("!/")) {
            return path.substring(0, path.length() - 2);
        }

        return path;
    }

    private static class NullProcessHandler extends ProcessHandler {
        public static NullProcessHandler INSTANCE = new NullProcessHandler();
        @Override
        protected void destroyProcessImpl() {
            throw new UnsupportedOperationException("destroyProcessImpl is not implemented");
        }

        @Override
        protected void detachProcessImpl() {
            throw new UnsupportedOperationException("detachProcessImpl is not implemented"); // TODO
        }

        @Override
        public boolean detachIsDefault() {
            throw new UnsupportedOperationException("detachIsDefault is not implemented");
        }

        @Override
        public OutputStream getProcessInput() {
            throw new UnsupportedOperationException("getProcessInput is not implemented");
        }
    }

    private static class CompilerProcessListener extends ProcessAdapter {
        private static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile("<(ERROR|WARNING|INFO|EXCEPTION)", Pattern.MULTILINE);
        private static final Pattern OPEN_TAG_END_PATTERN = Pattern.compile(">", Pattern.MULTILINE | Pattern.DOTALL);
        private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\s*(path|line|column)\\s*=\\s*\"(.*?)\"", Pattern.MULTILINE | Pattern.DOTALL);
        private static final Pattern MESSAGE_PATTERN = Pattern.compile("(.*?)</(ERROR|WARNING|INFO|EXCEPTION)>", Pattern.MULTILINE | Pattern.DOTALL);

        private enum State {
            WAITING, ATTRIBUTES, MESSAGE
        }

        private static class CompilerMessage {
            private CompilerMessageCategory messageCategory;
            private boolean isException;
            private @Nullable String url;
            private @Nullable Integer line;
            private @Nullable Integer column;
            private String message;

            public void setMessageCategoryFromString(String tagName) {
                boolean exception = "EXCEPTION".equals(tagName);
                if (Severity.ERROR.toString().equals(tagName) || exception) {
                    messageCategory = ERROR;
                    isException = exception;
                }
                else if (Severity.WARNING.toString().equals(tagName)) {
                    messageCategory = WARNING;
                }
                else {
                    messageCategory = INFORMATION;
                }
            }

            public void setAttributeFromStrings(String name, String value) {
                if ("path".equals(name)) {
                    url = "file://" + value.trim();
                }
                else if ("line".equals(name)) {
                    line = safeParseInt(value);
                }
                else if ("column".equals(name)) {
                    column = safeParseInt(value);
                }
            }

            @Nullable
            private static Integer safeParseInt(String value) {
                try {
                    return Integer.parseInt(value.trim());
                }
                catch (NumberFormatException e) {
                    return null;
                }
            }

            public void setMessage(String message) {
                this.message = message;
            }

            public void reportTo(CompileContext compileContext) {
                compileContext.addMessage(messageCategory, message, url == null ? "" : url, line == null ? -1 : line, column == null ? -1 : column);
                if (isException) {
                    LOG.error(message);
                }
            }
        }

        private final CompileContext compileContext;
        private final StringBuilder output = new StringBuilder();
        private int firstUnprocessedIndex = 0;
        private State state = State.WAITING;
        private CompilerMessage currentCompilerMessage;

        public CompilerProcessListener(CompileContext compileContext) {
            this.compileContext = compileContext;
        }

        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
            String text = event.getText();
            if (outputType == ProcessOutputTypes.STDERR) {
                output.append(text);

                // We loop until the state stabilizes
                State lastState;
                do {
                    lastState = state;
                    switch (state) {
                        case WAITING: {
                            Matcher matcher = matcher(DIAGNOSTIC_PATTERN);
                            if (find(matcher)) {
                                currentCompilerMessage = new CompilerMessage();
                                currentCompilerMessage.setMessageCategoryFromString(matcher.group(1));
                                state = State.ATTRIBUTES;
                            }
                            break;
                        }
                        case ATTRIBUTES: {
                            Matcher matcher = matcher(ATTRIBUTE_PATTERN);
                            int indexDelta = 0;
                            while (matcher.find()) {
                                handleSkippedOutput(output.subSequence(firstUnprocessedIndex + indexDelta, firstUnprocessedIndex + matcher.start()));
                                currentCompilerMessage.setAttributeFromStrings(matcher.group(1), matcher.group(2));
                                indexDelta = matcher.end();

                            }
                            firstUnprocessedIndex += indexDelta;

                            Matcher endMatcher = matcher(OPEN_TAG_END_PATTERN);
                            if (find(endMatcher)) {
                                state = State.MESSAGE;
                            }
                            break;
                        }
                        case MESSAGE: {
                            Matcher matcher = matcher(MESSAGE_PATTERN);
                            if (find(matcher)) {
                                currentCompilerMessage.setMessage(matcher.group(1));
                                currentCompilerMessage.reportTo(compileContext);
                                state = State.WAITING;
                            }
                            break;
                        }
                    }
                }
                while (state != lastState);

            }
            else {
                compileContext.addMessage(INFORMATION, text, "", -1, -1);
            }
        }

        private boolean find(Matcher matcher) {
            boolean result = matcher.find();
            if (result) {
                handleSkippedOutput(output.subSequence(firstUnprocessedIndex, firstUnprocessedIndex + matcher.start()));
                firstUnprocessedIndex += matcher.end();
            }
            return result;
        }

        private Matcher matcher(Pattern pattern) {
            return pattern.matcher(output.subSequence(firstUnprocessedIndex, output.length()));
        }

        @Override
        public void processTerminated(ProcessEvent event) {
            if (firstUnprocessedIndex < output.length()) {
                handleSkippedOutput(output.substring(firstUnprocessedIndex).trim());
            }
            int exitCode = event.getExitCode();
            // 0 is normal, 1 is "errors found" - handled by the messages above
            if (exitCode != 0 && exitCode != 1) {
                compileContext.addMessage(ERROR, "Compiler terminated with exit code: " + exitCode, "", -1, -1);
            }
        }

        private void handleSkippedOutput(CharSequence substring) {
            String message = substring.toString();
            if (!message.trim().isEmpty()) {
                compileContext.addMessage(ERROR, message, "", -1, -1);
            }
        }
    }
}
