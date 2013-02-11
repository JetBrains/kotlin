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

package org.jetbrains.jet.test.generator;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleTestMethodModel implements TestMethodModel {
    private final File rootDir;
    private final File file;
    private final String doTestMethodName;
    private final Pattern filenamePattern;

    public SimpleTestMethodModel(File rootDir, File file, String doTestMethodName, Pattern filenamePattern) {
        this.rootDir = rootDir;
        this.file = file;
        this.doTestMethodName = doTestMethodName;
        this.filenamePattern = filenamePattern;
    }

    @Override
    public void generateBody(@NotNull Printer p, @NotNull String generatorClassFqName) {
        p.println(doTestMethodName, "(\"", JetTestUtils.getFilePath(file), "\");");
    }

    @Override
    public String getDataString() {
        return JetTestUtils.getFilePath(new File(FileUtil.getRelativePath(rootDir, file)));
    }

    @Override
    public String getName() {
        Matcher matcher = filenamePattern.matcher(file.getName());
        boolean found = matcher.find();
        assert found : file.getName() + " isn't matched by regex " + filenamePattern.pattern();
        assert matcher.groupCount() == 1 : filenamePattern.pattern();
        String extractedName = matcher.group(1);

        String unescapedName;
        if (rootDir.equals(file.getParentFile())) {
            unescapedName = extractedName;
        }
        else {
            String relativePath = FileUtil.getRelativePath(rootDir, file.getParentFile());
            unescapedName = relativePath + "-" + StringUtil.capitalize(extractedName);
        }
        return "test" + StringUtil.capitalize(TestGeneratorUtil.escapeForJavaIdentifier(unescapedName));
    }
}
