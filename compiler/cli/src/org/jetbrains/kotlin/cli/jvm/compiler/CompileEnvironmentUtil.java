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

package org.jetbrains.kotlin.cli.jvm.compiler;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import kotlin.Unit;
import kotlin.io.FilesKt;
import kotlin.jvm.functions.Function1;
import kotlin.sequences.SequencesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk;
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.*;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompileEnvironmentUtil {
    private static final Logger LOG = Logger.getInstance(CompileEnvironmentUtil.class);

    @NotNull
    public static ModuleChunk loadModuleChunk(File buildFile, MessageCollector messageCollector) {
        if (!buildFile.exists()) {
            messageCollector.report(ERROR, "Module definition file does not exist: " + buildFile, null);
            return ModuleChunk.EMPTY;
        }
        if ("xml".equalsIgnoreCase(FilesKt.getExtension(buildFile))) {
            return ModuleXmlParser.parseModuleScript(buildFile.getPath(), messageCollector);
        }
        messageCollector.report(ERROR, "Unknown module definition type: " + buildFile, null);
        return ModuleChunk.EMPTY;
    }

    // TODO: includeRuntime should be not a flag but a path to runtime
    private static void doWriteToJar(OutputFileCollection outputFiles, OutputStream fos, @Nullable FqName mainClass, boolean includeRuntime) {
        try {
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass.asString());
            }
            JarOutputStream stream = new JarOutputStream(fos, manifest);
            for (OutputFile outputFile : outputFiles.asList()) {
                stream.putNextEntry(new JarEntry(outputFile.getRelativePath()));
                stream.write(outputFile.asByteArray());
            }
            if (includeRuntime) {
                writeRuntimeToJar(stream);
            }
            stream.finish();
        }
        catch (IOException e) {
            throw new CompileEnvironmentException("Failed to generate jar file", e);
        }
    }

    public static void writeToJar(File jarPath, boolean jarRuntime, FqName mainClass, OutputFileCollection outputFiles) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(jarPath);
            doWriteToJar(outputFiles, outputStream, mainClass, jarRuntime);
            outputStream.close();
        }
        catch (FileNotFoundException e) {
            throw new CompileEnvironmentException("Invalid jar path " + jarPath, e);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
        finally {
            ExceptionUtilsKt.closeQuietly(outputStream);
        }
    }

    private static void writeRuntimeToJar(JarOutputStream stream) throws IOException {
        File stdlibPath = PathUtil.getKotlinPathsForCompiler().getStdlibPath();
        if (!stdlibPath.exists()) {
            throw new CompileEnvironmentException("Couldn't find kotlin-stdlib at " + stdlibPath);
        }
        copyJarImpl(stream, stdlibPath);
    }

    private static void copyJarImpl(JarOutputStream stream, File jarPath) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarPath))) {
            while (true) {
                JarEntry e = jis.getNextJarEntry();
                if (e == null) {
                    break;
                }
                if (FileUtilRt.extensionEquals(e.getName(), "class")) {
                    stream.putNextEntry(e);
                    FileUtil.copy(jis, stream);
                }
            }
        }
    }

    @NotNull
    public static List<KtFile> getKtFiles(
            @NotNull Project project,
            @NotNull Collection<String> sourceRoots,
            @NotNull CompilerConfiguration configuration,
            @NotNull Function1<String, Unit> reportError
    ) throws IOException {
        VirtualFileSystem localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);

        Set<VirtualFile> processedFiles = new HashSet<>();
        List<KtFile> result = Lists.newArrayList();

        PreprocessedFileCreator virtualFileCreator = new PreprocessedFileCreator(project);

        for (String sourceRootPath : sourceRoots) {
            if (sourceRootPath == null) {
                continue;
            }

            VirtualFile vFile = localFileSystem.findFileByPath(sourceRootPath);
            if (vFile == null) {
                String message = "Source file or directory not found: " + sourceRootPath;

                File buildFilePath = configuration.get(JVMConfigurationKeys.MODULE_XML_FILE);
                if (buildFilePath != null && Logger.isInitialized()) {
                    LOG.warn(message +
                             "\n\nbuild file path: " + buildFilePath +
                             "\ncontent:\n" + FileUtil.loadFile(buildFilePath));
                }

                reportError.invoke(message);
                continue;
            }
            if (!vFile.isDirectory() && vFile.getFileType() != KotlinFileType.INSTANCE) {
                reportError.invoke("Source entry is not a Kotlin file: " + sourceRootPath);
                continue;
            }

            SequencesKt.forEach(FilesKt.walkTopDown(new File(sourceRootPath)), file -> {
                if (file.isFile()) {
                    VirtualFile originalVirtualFile = localFileSystem.findFileByPath(file.getAbsolutePath());
                    VirtualFile virtualFile = originalVirtualFile != null ? virtualFileCreator.create(originalVirtualFile) : null;
                    if (virtualFile != null && !processedFiles.contains(virtualFile)) {
                        processedFiles.add(virtualFile);
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                        if (psiFile instanceof KtFile) {
                            result.add((KtFile) psiFile);
                        }
                    }
                }
                return Unit.INSTANCE;
            });
        }

        return result;
    }
}
