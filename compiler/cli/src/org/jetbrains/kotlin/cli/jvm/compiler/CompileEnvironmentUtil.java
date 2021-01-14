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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import kotlin.io.FilesKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk;
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompileEnvironmentUtil {

    public static long DOS_EPOCH = new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis();

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
    private static void doWriteToJar(
            OutputFileCollection outputFiles,
            OutputStream fos,
            @Nullable FqName mainClass,
            boolean includeRuntime,
            boolean noReflect,
            boolean resetJarTimestamps
    ) {
        try {
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass.asString());
            }

            JarOutputStream stream = new JarOutputStream(fos);
            JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
            if (resetJarTimestamps) {
                manifestEntry.setTime(DOS_EPOCH);
            }
            stream.putNextEntry(manifestEntry);
            manifest.write(new BufferedOutputStream(stream));

            for (OutputFile outputFile : outputFiles.asList()) {
                JarEntry entry = new JarEntry(outputFile.getRelativePath());
                if (resetJarTimestamps) {
                    entry.setTime(DOS_EPOCH);
                }
                stream.putNextEntry(entry);
                stream.write(outputFile.asByteArray());
            }
            if (includeRuntime) {
                writeRuntimeToJar(stream, resetJarTimestamps);
                if (!noReflect) {
                    writeReflectToJar(stream, resetJarTimestamps);
                }
            }
            stream.finish();
        }
        catch (IOException e) {
            throw new CompileEnvironmentException("Failed to generate jar file", e);
        }
    }

    public static void writeToJar(
            File jarPath, boolean jarRuntime, boolean noReflect, boolean resetJarTimestamps, FqName mainClass, OutputFileCollection outputFiles
    ) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(jarPath);
            doWriteToJar(outputFiles, outputStream, mainClass, jarRuntime, noReflect, resetJarTimestamps);
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

    private static void writeRuntimeToJar(JarOutputStream stream, boolean resetJarTimestamps) throws IOException {
        File stdlibPath = PathUtil.getKotlinPathsForCompiler().getStdlibPath();
        if (!stdlibPath.exists()) {
            throw new CompileEnvironmentException("Couldn't find kotlin-stdlib at " + stdlibPath);
        }
        copyJarImpl(stream, stdlibPath, resetJarTimestamps);
    }

    private static void writeReflectToJar(JarOutputStream stream, boolean resetJarTimestamps) throws IOException {
        File reflectPath = PathUtil.getKotlinPathsForCompiler().getReflectPath();
        if (!reflectPath.exists()) {
            throw new CompileEnvironmentException("Couldn't find kotlin-reflect at " + reflectPath);
        }
        copyJarImpl(stream, reflectPath, resetJarTimestamps);
    }

    private static void copyJarImpl(JarOutputStream stream, File jarPath, boolean resetJarTimestamps) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarPath))) {
            while (true) {
                JarEntry e = jis.getNextJarEntry();
                if (e == null) break;

                if ((!FileUtilRt.extensionEquals(e.getName(), "class") &&
                     !FileUtilRt.extensionEquals(e.getName(), BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION)) ||
                    StringsKt.substringAfterLast(e.getName(), "/", e.getName()).equals("module-info.class")) {
                    continue;
                }
                if (resetJarTimestamps) {
                    e.setTime(DOS_EPOCH);
                }
                stream.putNextEntry(e);
                FileUtil.copy(jis, stream);
            }
        }
    }
}
