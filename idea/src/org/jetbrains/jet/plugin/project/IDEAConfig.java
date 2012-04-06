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

package org.jetbrains.jet.plugin.project;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Pavel Talanov
 */
public final class IDEAConfig extends Config {

    @Nullable
    private final String pathToLibZip;

    public IDEAConfig(@NotNull Project project) {
        super(project);
        this.pathToLibZip = getLibLocationForProject(project);
    }

    //TODO: refactor
    @Nullable
    private static String getLibLocationForProject(@NotNull Project project) {
        VirtualFile indicationFile = JsModuleDetector.findIndicationFileInContextRoots(project);
        if (indicationFile == null) {
            return null;
        }
        try {
            InputStream stream = indicationFile.getInputStream();
            String path = FileUtil.loadTextAndClose(stream);
            String pathToLibFile = getFirstLine(path);
            if (pathToLibFile == null) {
                return null;
            }
            try {
                URI pathToLibFileUri = new URI(pathToLibFile);
                URI pathToIndicationFileUri = new URI(indicationFile.getPath());
                return pathToIndicationFileUri.resolve(pathToLibFileUri).toString();
            }
            catch (URISyntaxException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return null;
            }
        }
        catch (IOException e) {
            return null;
        }
    }

    //TODO: util
    @Nullable
    private static String getFirstLine(@NotNull String path) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(path));
        try {
            return reader.readLine();
        }

        finally {
            reader.close();
        }
    }

    @NotNull
    @Override
    public List<JetFile> generateLibFiles() {
        if (pathToLibZip == null) {
            return Collections.emptyList();
        }
        try {
            File file = new File(pathToLibZip);
            ZipFile zipFile = new ZipFile(file);
            try {
                return traverseArchive(zipFile);
            }
            finally {
                zipFile.close();
            }
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @NotNull
    private List<JetFile> traverseArchive(@NotNull ZipFile file) throws IOException {
        List<JetFile> result = Lists.newArrayList();
        Enumeration<? extends ZipEntry> zipEntries = file.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            if (!entry.isDirectory()) {
                InputStream stream = file.getInputStream(entry);
                String text = FileUtil.loadTextAndClose(stream);
                JetFile jetFile = JetFileUtils.createPsiFile(entry.getName(), text, getProject());
                result.add(jetFile);
            }
        }
        return result;
    }
}
