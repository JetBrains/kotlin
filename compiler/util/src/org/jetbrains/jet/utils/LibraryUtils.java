/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LibraryUtils {
    private static final Logger LOG = Logger.getInstance(LibraryUtils.class);

    public static final String KOTLIN_JS_MODULE_NAME = "Kotlin-JS-Module-Name";
    private static final String TITLE_KOTLIN_JAVASCRIPT_STDLIB;
    private static final String TITLE_KOTLIN_JAVASCRIPT_LIB;
    private static final String JS_EXT = ".js";
    private static final String METAINF = "META-INF/";
    private static final String MANIFEST_PATH = METAINF + "MANIFEST.MF";
    private static final String METAINF_RESOURCES = METAINF + "resources/";
    private static final Attributes.Name KOTLIN_JS_MODULE_ATTRIBUTE_NAME = new Attributes.Name(KOTLIN_JS_MODULE_NAME);

    static {
        String jsStdLib = "";
        String jsLib = "";

        InputStream manifestProperties = LibraryUtils.class.getResourceAsStream("/manifest.properties");
        if (manifestProperties != null) {
            try {
                Properties properties = new Properties();
                properties.load(manifestProperties);
                jsStdLib = properties.getProperty("manifest.impl.title.kotlin.javascript.stdlib");
                jsLib = properties.getProperty("manifest.spec.title.kotlin.javascript.lib");
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
        else {
            LOG.error("Resource 'manifest.properties' not found.");
        }

        TITLE_KOTLIN_JAVASCRIPT_STDLIB = jsStdLib;
        TITLE_KOTLIN_JAVASCRIPT_LIB = jsLib;
    }

    private LibraryUtils() {}

    public static VirtualFile getJarFile(@NotNull List<VirtualFile> classesRoots, @NotNull String jarName) {
        for (VirtualFile root : classesRoots) {
            if (root.getName().equals(jarName)) {
                return root;
            }
        }

        return null;
    }

    @Nullable
    public static String getKotlinJsModuleName(@NotNull File library) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        return attributes != null ? attributes.getValue(KOTLIN_JS_MODULE_ATTRIBUTE_NAME) : null;
    }

    public static boolean isKotlinJavascriptLibrary(@NotNull File library) {
        return checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_LIB, Attributes.Name.SPECIFICATION_TITLE);
    }

    public static boolean isKotlinJavascriptStdLibrary(@NotNull File library) {
        return checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_STDLIB, Attributes.Name.IMPLEMENTATION_TITLE);
    }

    public static void copyJsFilesFromLibraries(@NotNull List<String> libraries, @NotNull String outputLibraryJsPath) {
        for(String library : libraries) {
            File file = new File(library);
            assert file.exists() : "Library " + library + " not found";

            if (file.isDirectory()) {
                copyJsFilesFromDirectory(file, outputLibraryJsPath);
            }
            else {
                copyJsFilesFromZip(file, outputLibraryJsPath);
            }
        }
    }

    private static void copyJsFilesFromDirectory(@NotNull final File dir, @NotNull final String outputLibraryJsPath) {
        FileUtil.processFilesRecursively(dir, new Processor<File>() {
            @Override
            public boolean process(File file) {
                String relativePath = FileUtil.getRelativePath(dir, file);
                assert relativePath != null : "relativePath should not be null " + dir + " " + file;
                if (file.isFile() && relativePath.endsWith(JS_EXT)) {
                    relativePath = getSuggestedPath(relativePath);
                    if (relativePath == null) return true;

                    try {
                        FileUtil.copy(file, new File(outputLibraryJsPath, relativePath));
                    }
                    catch (IOException ex) {
                        LOG.error("Could not copy " + relativePath + " from " + dir + ": " + ex.getMessage());
                    }
                }
                return true;
            }
        });
    }

    private static void copyJsFilesFromZip(@NotNull File file, @NotNull String outputLibraryJsPath) {
        try {
            ZipFile zipFile = new ZipFile(file.getPath());
            try {
                traverseArchive(zipFile, outputLibraryJsPath);
            }
            finally {
                zipFile.close();
            }
        }
        catch (IOException ex) {
            LOG.error("Could extract javascript files from  " + file.getName() + ": " + ex.getMessage());
        }
    }

    private static void traverseArchive(@NotNull ZipFile zipFile, @NotNull String outputLibraryJsPath) throws IOException {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.endsWith(JS_EXT)) {
                String relativePath = getSuggestedPath(entryName);
                if (relativePath == null) continue;

                InputStream stream = zipFile.getInputStream(entry);
                String content = FileUtil.loadTextAndClose(stream);
                File outputFile = new File(outputLibraryJsPath, relativePath);
                FileUtil.writeToFile(outputFile, content);
            }
        }
    }

    @Nullable
    private static String getSuggestedPath(@NotNull String path) {
        if (path.startsWith(METAINF)) {
            if (path.startsWith(METAINF_RESOURCES)) {
                return path.substring(METAINF_RESOURCES.length());
            }
            return null;
        }

        return path;
    }

    @Nullable
    private static Manifest getManifestFromJar(@NotNull File library) {
        if (!library.canRead()) return null;

        try {
            JarFile jarFile = new JarFile(library);
            try {
                return jarFile.getManifest();
            }
            finally {
                jarFile.close();
            }
        }
        catch (IOException ignored) {
            return null;
        }
    }

    @Nullable
    private static Manifest getManifestFromDirectory(@NotNull File library) {
        if (!library.canRead() || !library.isDirectory()) return null;

        File manifestFile = new File(library, MANIFEST_PATH);
        if (!manifestFile.exists()) return null;

        try {
            InputStream inputStream = new FileInputStream(manifestFile);
            try {
                return new Manifest(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException ignored) {
            LOG.warn("IOException " + ignored);
            return null;
        }
    }

    private static Manifest getManifestFromJarOrDirectory(@NotNull File library) {
        return library.isDirectory() ? getManifestFromDirectory(library) : getManifestFromJar(library);
    }

    @Nullable
    private static Attributes getManifestMainAttributesFromJarOrDirectory(@NotNull File library) {
        Manifest manifest = getManifestFromJarOrDirectory(library);
        return manifest != null ? manifest.getMainAttributes() : null;
    }

    private static boolean checkAttributeValue(@NotNull File library, String expected, @NotNull Attributes.Name attributeName) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        if (attributes == null) return false;

        String value = attributes.getValue(attributeName);
        return value != null && value.equals(expected);
    }
}