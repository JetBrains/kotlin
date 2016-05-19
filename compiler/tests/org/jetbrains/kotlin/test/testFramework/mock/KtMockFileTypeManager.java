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

package org.jetbrains.kotlin.test.testFramework.mock;

import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class KtMockFileTypeManager extends FileTypeManager {
    private final FileType fileType;

    public KtMockFileTypeManager(FileType fileType) {
        this.fileType = fileType;
    }

    @Override
    @NotNull
    public String getIgnoredFilesList() {
        throw new IncorrectOperationException();
    }

    @Override
    public void setIgnoredFilesList(@NotNull String list) {
    }

    @Override
    public void registerFileType(@NotNull FileType type, @NotNull List<FileNameMatcher> defaultAssociations) {
    }

    @Override
    @NotNull
    public FileType getFileTypeByFileName(@NotNull String fileName) {
        return fileType;
    }

    @Override
    @NotNull
    public FileType getFileTypeByFile(@NotNull VirtualFile file) {
        return fileType;
    }

    @Override
    @NotNull
    public FileType getFileTypeByExtension(@NotNull String extension) {
        return fileType;
    }

    @Override
    @NotNull
    public FileType[] getRegisteredFileTypes() {
        return FileType.EMPTY_ARRAY;
    }

    @Override
    public boolean isFileIgnored(@NotNull String name) {
        return false;
    }

    @Override
    public boolean isFileIgnored(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    @NotNull
    public String[] getAssociatedExtensions(@NotNull FileType type) {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public void addFileTypeListener(@NotNull FileTypeListener listener) {
    }

    @Override
    public void removeFileTypeListener(@NotNull FileTypeListener listener) {
    }

    @Override
    public FileType getKnownFileTypeOrAssociate(@NotNull VirtualFile file) {
        return file.getFileType();
    }

    @Override
    public FileType getKnownFileTypeOrAssociate(@NotNull VirtualFile file, @NotNull Project project) {
        return getKnownFileTypeOrAssociate(file);
    }

    @Override
    @NotNull
    public List<FileNameMatcher> getAssociations(@NotNull FileType type) {
        return Collections.emptyList();
    }

    @Override
    public void associate(@NotNull FileType type, @NotNull FileNameMatcher matcher) {
    }

    @Override
    public void removeAssociation(@NotNull FileType type, @NotNull FileNameMatcher matcher) {
    }

    @Override
    @NotNull
    public FileType getStdFileType(@NotNull @NonNls String fileTypeName) {
        if ("ARCHIVE".equals(fileTypeName) || "CLASS".equals(fileTypeName)) return UnknownFileType.INSTANCE;
        if ("PLAIN_TEXT".equals(fileTypeName)) return PlainTextFileType.INSTANCE;
        if ("JAVA".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.JavaFileType", fileTypeName);
        if ("XML".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.XmlFileType", fileTypeName);
        if ("DTD".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.DTDFileType", fileTypeName);
        if ("JSP".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.NewJspFileType", fileTypeName);
        if ("JSPX".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.JspxFileType", fileTypeName);
        if ("HTML".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.HtmlFileType", fileTypeName);
        if ("XHTML".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.ide.highlighter.XHtmlFileType", fileTypeName);
        if ("JavaScript".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.lang.javascript.JavaScriptFileType", fileTypeName);
        if ("Properties".equals(fileTypeName)) return loadFileTypeSafe("com.intellij.lang.properties.PropertiesFileType", fileTypeName);
        return new KtMockLanguageFileType(PlainTextLanguage.INSTANCE, fileTypeName.toLowerCase());
    }

    private static FileType loadFileTypeSafe(String className, String fileTypeName) {
        try {
            return (FileType)Class.forName(className).getField("INSTANCE").get(null);
        }
        catch (Exception ignored) {
            return new KtMockLanguageFileType(PlainTextLanguage.INSTANCE, fileTypeName.toLowerCase(Locale.ENGLISH));
        }
    }

    @Override
    public boolean isFileOfType(@NotNull VirtualFile file, @NotNull FileType type) {
        return false;
    }

    @NotNull
    @Override
    public FileType detectFileTypeFromContent(@NotNull VirtualFile file) {
        return UnknownFileType.INSTANCE;
    }

    @Nullable
    @Override
    public FileType findFileTypeByName(@NotNull String fileTypeName) {
        return null;
    }
}