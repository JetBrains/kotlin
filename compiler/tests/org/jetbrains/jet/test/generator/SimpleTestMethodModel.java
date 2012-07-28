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

package org.jetbrains.jet.test.generator;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;

import java.io.File;

/**
* @author abreslav
*/
public class SimpleTestMethodModel implements TestMethodModel {
    private final File rootDir;
    private final File file;
    private final String doTestMethodName;

    public SimpleTestMethodModel(File rootDir, File file, String doTestMethodName) {
        this.rootDir = rootDir;
        this.file = file;
        this.doTestMethodName = doTestMethodName;
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
        String fileName = FileUtil.getNameWithoutExtension(file.getName());
        String unescapedName;
        if (rootDir.equals(file.getParentFile())) {
            unescapedName = fileName;
        }
        else {
            String relativePath = FileUtil.getRelativePath(rootDir, file.getParentFile());
            unescapedName = relativePath + "-" + StringUtil.capitalize(fileName);
        }
        return "test" + StringUtil.capitalize(TestGeneratorUtil.escapeForJavaIdentifier(unescapedName));
    }
}
