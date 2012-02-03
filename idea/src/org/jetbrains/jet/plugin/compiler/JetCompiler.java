package org.jetbrains.jet.plugin.compiler;

import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
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
import org.jetbrains.jet.plugin.JetFileType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.compiler.CompilerMessageCategory.*;

/**
 * @author yole
 */
public class JetCompiler implements TranslatingCompiler {
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

        final Module module = compileContext.getModuleByFile(virtualFiles[0]);
        final VirtualFile outputDir = compileContext.getModuleOutputDirectory(module);
        if (outputDir == null) {
            compileContext.addMessage(ERROR, "[Internal Error] No output directory", "", -1, -1);
            return;
        }

        File kotlinHome = PathUtil.getDefaultCompilerPath();
        if (kotlinHome == null) {
            compileContext.addMessage(ERROR, "Cannot find kotlinc home. Make sure plugin is properly installed", "", -1, -1);
            return;
        }
        
        
        StringBuilder script = new StringBuilder();

        script.append("import kotlin.modules.*\n");
        script.append("fun project() {\n");
        script.append("module(\"" + moduleChunk.getNodes().iterator().next().getName() + "\") {\n");

        for (VirtualFile sourceFile : virtualFiles) {
            script.append("sources += \"" + path(sourceFile) + "\"\n");
        }

        ModuleChunk chunk = new ModuleChunk((CompileContextEx) compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());

        // TODO: have a bootclasspath in script API
        for (VirtualFile root : chunk.getCompilationBootClasspathFiles()) {
            script.append("classpath += \"" + path(root) + "\"\n");
        }

        for (VirtualFile root : chunk.getCompilationClasspathFiles()) {
            script.append("classpath += \"" + path(root) + "\"\n");
        }

        script.append("}\n");
        script.append("}\n");

        File scriptFile = new File(path(outputDir), "script.kts");
        try {
            FileUtil.writeToFile(scriptFile, script.toString());
        } catch (IOException e) {
            compileContext.addMessage(ERROR, "[Internal Error] Cannot write script to " + scriptFile.getAbsolutePath(), "", -1, -1);
            return;
        }


        final SimpleJavaParameters params = new SimpleJavaParameters();
        params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        params.setMainClass("org.jetbrains.jet.cli.KotlinCompiler");
        params.getProgramParametersList().add("-module", scriptFile.getAbsolutePath());
        params.getProgramParametersList().add("-output", path(outputDir));

        File libs = new File(kotlinHome, "lib");

        if (!libs.exists() || libs.isFile()) {
            compileContext.addMessage(ERROR, "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed", "", -1, -1);
            return;
        }

        File[] jars = libs.listFiles();
        if (jars != null) {
            for (File jar : jars) {
                if (jar.isFile() && jar.getName().endsWith(".jar")) {
                    params.getClassPath().add(jar);
                }
            }
        }
        
        params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
        //params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

        Sdk sdk = params.getJdk();

        final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(
                ((JavaSdkType) sdk.getSdkType()).getVMExecutablePath(sdk), params, false);
        try {
            final OSProcessHandler processHandler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString()) {
              @Override
              public Charset getCharset() {
                return commandLine.getCharset();
              }
            };

            processHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(ProcessEvent event, Key outputType) {
                    String text = event.getText();
                    String levelCode = parsePrefix(text);
                    if (levelCode != null) {
                        CompilerMessageCategory category = categories.get(levelCode);
                        text = text.substring(levelCode.length());

                        String path = "";
                        int line = -1;
                        int column = -1;
                        int colonIndex = text.indexOf(':');
                        if (colonIndex > 0) {
                            path = "file://" + text.substring(0, colonIndex).trim();
                            text = text.substring(colonIndex + 1);

                            Pattern position = Pattern.compile("\\((\\d+),\\s*(\\d+)\\)");

                            Matcher matcher = position.matcher(text);
                            if (matcher.find()) {
                                line = Integer.parseInt(matcher.group(1));
                                column = Integer.parseInt(matcher.group(2));
                                text = text.substring(matcher.group(0).length());
                            }
                        }

                        compileContext.addMessage(category, text, path, line, column);
                    }
                    else {
                        compileContext.addMessage(INFORMATION, text, "", -1, -1);
                    }
                }
            });

            processHandler.startNotify();
            processHandler.waitFor();
        } catch (Exception e) {
            compileContext.addMessage(ERROR, "[Internal Error] " + e.getLocalizedMessage(), "", -1, -1);
            return;
        }
    }

    private static String[] messagePrefixes = new String[] {"ERROR:", "WARNING:", "INFO:"} ;
    private static Map<String, CompilerMessageCategory> categories = new HashMap<String, CompilerMessageCategory>();
    static {
        categories.put("ERROR:", ERROR);
        categories.put("WARNING:", WARNING);
        categories.put("INFORMATION:", INFORMATION);
    }

    private static String parsePrefix(String message) {
        for (String prefix : messagePrefixes) {
            if (message.startsWith(prefix)) return prefix;
        }
        return null;
    }
    
    private static String path(VirtualFile root) {
        String path = root.getPath();
        if (path.endsWith("!/")) {
            return path.substring(0, path.length() - 2);
        }

        return path;
    }
}
