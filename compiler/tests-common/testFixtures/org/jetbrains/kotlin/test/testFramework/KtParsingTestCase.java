/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.lang.ParserDefinition;
import com.intellij.mock.MockProject;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.TestDataFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("ALL")
public abstract class KtParsingTestCase extends KtPlatformLiteFixture {
    protected String myFileExt;
    protected final String myFullDataPath;
    protected PsiFile myFile;
    private final ParserDefinition[] myDefinitions;
    private final boolean myLowercaseFirstLetter;

    private KotlinCoreEnvironment myEnvironment;

    protected KtParsingTestCase(@NonNls @NotNull String dataPath, @NotNull String fileExt, @NotNull ParserDefinition... definitions) {
        this(dataPath, fileExt, false, definitions);
    }

    protected KtParsingTestCase(@NonNls @NotNull String dataPath, @NotNull String fileExt, boolean lowercaseFirstLetter, @NotNull ParserDefinition... definitions) {
        myDefinitions = definitions;
        myFullDataPath = getTestDataPath() + "/" + dataPath;
        myFileExt = fileExt;
        myLowercaseFirstLetter = lowercaseFirstLetter;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myEnvironment = KotlinCoreEnvironment.createForTests(getTestRootDisposable(), CompilerConfiguration.EMPTY,
                                                             EnvironmentConfigFiles.JVM_CONFIG_FILES);
        myProject = (MockProject) myEnvironment.getProject();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myFile = null;
        myProject = null;
        myEnvironment = null;
    }

    protected String getTestDataPath() {
        return PathManager.getHomePath();
    }

    @NotNull
    public final String getTestName() {
        return getTestName(myLowercaseFirstLetter);
    }

    protected boolean includeRanges() {
        return false;
    }

    protected boolean skipSpaces() {
        return false;
    }

    protected boolean checkAllPsiRoots() {
        return true;
    }

    protected PsiFile createPsiFile(String name, String text) {
        return createFile(name + "." + myFileExt, text);
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        return KtTestUtil.createFile(name, text, myProject);
    }

    public static void doCheckResult(String fullPath, String targetDataName, String actual) throws IOException {
        String expectedFileName = fullPath + File.separatorChar + targetDataName;
        KtUsefulTestCase.assertSameLinesWithFile(expectedFileName, actual);
    }

    protected static String toParseTreeText(PsiElement file,  boolean skipSpaces, boolean printRanges) {
        boolean showWhitespaces = !skipSpaces;
        return DebugUtil.psiToString(file, showWhitespaces, printRanges);
    }

    protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
        return loadFileDefault(myFullDataPath, name);
    }

    public static String loadFileDefault(String dir, String name) throws IOException {
        return FileUtil.loadFile(new File(dir, name), CharsetToolkit.UTF8, true).trim();
    }
}
