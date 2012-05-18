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

package org.jetbrains.jet;

import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.junit.Assert;

import java.io.IOException;

/**
 * @author abreslav
 */
public abstract class JetLiteFixture extends UsefulTestCase {
    @NonNls
    protected final String myFullDataPath;
    protected JetFile myFile;
    protected JetCoreEnvironment myEnvironment;

    public JetLiteFixture(@NonNls String dataPath) {
        myFullDataPath = getTestDataPath() + "/" + dataPath;
    }

    public JetLiteFixture() {
        myFullDataPath = getTestDataPath();
    }

    protected String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase();
    }

    public Project getProject() {
        return myEnvironment.getProject();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations() {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable());
    }

    protected void createEnvironmentWithMockJdkAndIdeaAnnotations(@NotNull CompilerSpecialMode compilerSpecialMode) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(), compilerSpecialMode);
    }

    protected void createEnvironmentWithFullJdk() {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }
        myEnvironment = JetTestUtils.createEnvironmentWithFullJdk(getTestRootDisposable());
    }

    @Override
    protected void tearDown() throws Exception {
        myFile = null;
        myEnvironment = null;
        super.tearDown();
    }

    protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
        return JetTestUtils.doLoadFile(myFullDataPath, name);
    }

    protected JetFile createPsiFile(@Nullable String testName, @Nullable String fileName, String text) {
        if (fileName == null) {
            Assert.assertNotNull(testName);
            fileName = testName + ".jet";
        }
        return (JetFile) JetTestUtils.createFile(fileName, text, myEnvironment.getProject());
    }

    protected JetFile loadPsiFile(String name) {
        try {
            return createPsiFile(name, null, loadFile(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    protected void prepareForTest(String name) throws IOException {
        String text = loadFile(name + ".jet");
        createAndCheckPsiFile(name, text);
    }

    protected void createAndCheckPsiFile(String name, String text) {
        myFile = createCheckAndReturnPsiFile(name, null, text);
    }

    protected JetFile createCheckAndReturnPsiFile(String testName, String fileName, String text) {
        JetFile myFile = createPsiFile(testName, fileName, text);
        ensureParsed(myFile);
        assertEquals("light virtual file text mismatch", text, ((LightVirtualFile) myFile.getVirtualFile()).getContent().toString());
        assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(myFile.getVirtualFile()));
        assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText());
        assertEquals("psi text mismatch", text, myFile.getText());
        return myFile;
    }
}
