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

package org.jetbrains.jet.plugin.stubs;

import com.intellij.lang.FileASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.stubs.StubElement;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetFileStubBuilder;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;

import java.util.List;

/**
 * @author yole
 */
public class JetStubsTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    public void testSuperclassNames() {
        final PsiFile psiFile = myFixture.configureByText("foo.kt", "import java.util.ArrayList as alist\nclass C(): alist() { }");
        final List<JetDeclaration> declarations = ((JetFile) psiFile).getDeclarations();
        final JetClass jetClass = (JetClass) declarations.get(0);
        final PsiJetClassStub stub = JetStubElementTypes.CLASS.createStub(jetClass, null);
        final List<String> names = stub.getSuperNames();
        assertSameElements(names, "ArrayList", "alist");
    }

    public void testClassIsTrait() {
        PsiFile psiFile = myFixture.configureByText("foo.kt", "trait Test { }");
        final List<JetDeclaration> declarations = ((JetFile) psiFile).getDeclarations();
        final JetClass jetClass = (JetClass) declarations.get(0);
        final PsiJetClassStub stub = JetStubElementTypes.CLASS.createStub(jetClass, null);
        assertEquals(true, stub.isTrait());
    }

    public void testFilePackage() {
        doBuildTest("package some.test",
                    "PsiJetFileStubImpl[package=some.test]\n");
    }

    public void testClassTypeParameters() {
        doBuildTest("class C<T> { }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[name=C fqn=C superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n" +
                    "      TYPE_PARAMETER:PsiJetTypeParameterStubImpl[name=T extendText=null]\n");
    }

    public void testFunctionParameters() {
        doBuildTest("fun some(t: Int, other: String = \"hello\") { }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top name=some]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n" +
                    "      VALUE_PARAMETER:PsiJetParameterStubImpl[val name=t typeText=Int defaultValue=null]\n" +
                    "      VALUE_PARAMETER:PsiJetParameterStubImpl[val name=other typeText=String defaultValue=\"hello\"]\n");
    }

    public void testNotStoreInFunction() {
        doBuildTest("fun some() { val test = 12;\n fun fake() {}\n class FakeClass\n }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top name=some]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    private void doBuildTest(@NonNls final String source, @NonNls @NotNull final String tree) {
        final JetFile file = (JetFile) createLightFile(JetFileType.INSTANCE, source);
        final FileASTNode fileNode = file.getNode();
        assertNotNull(fileNode);
        // assertFalse(fileNode.isParsed()); // TODO

        JetFileStubBuilder jetStubBuilder = new JetFileStubBuilder();

        final StubElement lighterTree = jetStubBuilder.buildStubTree(file);
        // assertFalse(fileNode.isParsed()); // TODO

        final String lightStr = DebugUtil.stubTreeToString(lighterTree);

        assertEquals("light tree differs", tree, lightStr);
    }
}
