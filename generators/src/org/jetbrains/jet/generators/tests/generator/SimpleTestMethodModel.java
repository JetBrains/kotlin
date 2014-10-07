/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.tests.generator;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.jet.generators.tests.generator.TestGenerator.TargetBackend;

public class SimpleTestMethodModel implements TestMethodModel {
    @NotNull
    private final File rootDir;
    @NotNull
    private final File file;
    @NotNull
    private final String doTestMethodName;
    @NotNull
    private final Pattern filenamePattern;
    @NotNull
    private final TargetBackend targetBackend;

    public SimpleTestMethodModel(
            @NotNull File rootDir,
            @NotNull File file,
            @NotNull String doTestMethodName,
            @NotNull Pattern filenamePattern,
            @NotNull TargetBackend targetBackend
    ) {
        this.rootDir = rootDir;
        this.file = file;
        this.doTestMethodName = doTestMethodName;
        this.filenamePattern = filenamePattern;
        this.targetBackend = targetBackend;
    }

    @Override
    public void generateBody(@NotNull Printer p) {
        String filePath = JetTestUtils.getFilePath(file) + (file.isDirectory() ? "/" : "");
        p.println("String fileName = JetTestUtils.navigationMetadata(\"", filePath, "\");");
        p.println(doTestMethodName, "(fileName);");
    }

    @Override
    public String getDataString() {
        return JetTestUtils.getFilePath(new File(FileUtil.getRelativePath(rootDir, file)));
    }

    private boolean isIgnored() {
        if (targetBackend == TargetBackend.ANY) return false;

        try {
            String fileText = FileUtil.loadFile(file);
            List<String> backends = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// TARGET_BACKEND: ");
            return backends.size() > 0 && !targetBackend.name().equals(backends.get(0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        Matcher matcher = filenamePattern.matcher(file.getName());
        boolean found = matcher.find();
        assert found : file.getName() + " isn't matched by regex " + filenamePattern.pattern();
        assert matcher.groupCount() == 1 : filenamePattern.pattern();
        String extractedName = matcher.group(1);
        assert extractedName != null : "extractedName should not be null: "  + filenamePattern.pattern();

        String unescapedName;
        if (rootDir.equals(file.getParentFile())) {
            unescapedName = extractedName;
        }
        else {
            String relativePath = FileUtil.getRelativePath(rootDir, file.getParentFile());
            unescapedName = relativePath + "-" + StringUtil.capitalize(extractedName);
        }
        return (isIgnored() ? "ignored" : "test") + StringUtil.capitalize(TestGeneratorUtil.escapeForJavaIdentifier(unescapedName));
    }
}
