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

    public void testClassObject() {
        doBuildTest("class C { class object { fun foo() {} }}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[name=C fqn=C superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n" +
                    "    OBJECT_DECLARATION:PsiJetObjectStubImpl[class-object name=null fqName=null]\n" +
                    "      FUN:PsiJetFunctionStubImpl[name=foo]\n" +
                    "        VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testFunctionInNotNamedObject() {
        doBuildTest("object { fun testing() = 12 }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[name=testing]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testFunctionParameters() {
        doBuildTest("fun some(t: Int, other: String = \"hello\") { }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=some name=some]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n" +
                    "      VALUE_PARAMETER:PsiJetParameterStubImpl[val name=t typeText=Int defaultValue=null]\n" +
                    "      VALUE_PARAMETER:PsiJetParameterStubImpl[val name=other typeText=String defaultValue=\"hello\"]\n");
    }

    public void testNotStoreInFunction() {
        doBuildTest("fun some() { val test = 12;\n fun fake() {}\n class FakeClass\n }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=some name=some]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testPackageProperty() {
        doBuildTest("package test.testing\n" +
                    "val some = 12",
                    "PsiJetFileStubImpl[package=test.testing]\n" +
                    "  PROPERTY:PsiJetPropertyStubImpl[val top topFQName=test.testing.some name=some typeText=null bodyText=12]\n");
    }

    public void testClassProperty() {
        doBuildTest("class More { \n" +
                    "  private val test : Int = 11\n" +
                    "}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[name=More fqn=More superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n" +
                    "    PROPERTY:PsiJetPropertyStubImpl[val name=test typeText=Int bodyText=11]\n");
    }

    public void testNotStorePropertyFromInitializer() {
        doBuildTest("fun DoubleArray.some() = for (element in this) println(element)",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=some ext name=some]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testNotStorePropertiesFrom() {
        doBuildTest("class Test() {\n" +
                    "    val test = 12;\n" +
                    "    {\n" +
                    "        for (i in 0..12) {\n" +
                    "        }\n" +
                    "    }\n" +
                    "    fun more() {\n" +
                    "    }\n" +
                    "}\n",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[name=Test fqn=Test superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n" +
                    "    PROPERTY:PsiJetPropertyStubImpl[val name=test typeText=null bodyText=12]\n" +
                    "    FUN:PsiJetFunctionStubImpl[name=more]\n" +
                    "      VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testSimpleEnumBuild() {
        doBuildTest("enum class Test { First\n Second\n }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[enumClass name=Test fqn=Test superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n" +
                    "    ENUM_ENTRY:PsiJetClassStubImpl[enumEntry name=First fqn=Test.First superNames=[]]\n" +
                    "    ENUM_ENTRY:PsiJetClassStubImpl[enumEntry name=Second fqn=Test.Second superNames=[]]\n"
        );
    }

    public void testAnnotationClass() {
        doBuildTest("annotation class Test",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[isAnnotation name=Test fqn=Test superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n");
    }

    public void testInnerClass() {
        doBuildTest("class A { inner class B { } }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[name=A fqn=A superNames=[]]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n" +
                    "    CLASS:PsiJetClassStubImpl[inner name=B fqn=A.B superNames=[]]\n" +
                    "      TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n");
    }

    public void testNamedObject() {
        doBuildTest("object Test {}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  OBJECT_DECLARATION:PsiJetObjectStubImpl[top name=Test fqName=Test]\n");
    }

    public void testAnnotationOnClass() {
        doBuildTest("Deprecated class Test {}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  CLASS:PsiJetClassStubImpl[name=Test fqn=Test superNames=[]]\n" +
                    "    ANNOTATION_ENTRY:PsiJetAnnotationStubImpl[shortName=Deprecated]\n" +
                    "    TYPE_PARAMETER_LIST:PsiJetTypeParameterListStubImpl\n");
    }

    public void testAnnotationOnFunction() {
        doBuildTest("Deprecated fun foo() {}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=foo name=foo]\n" +
                    "    ANNOTATION_ENTRY:PsiJetAnnotationStubImpl[shortName=Deprecated]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testQualifiedAnnotationOnFunction() {
        doBuildTest("java.lang.Deprecated fun foo() {}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=foo name=foo]\n" +
                    "    ANNOTATION_ENTRY:PsiJetAnnotationStubImpl[shortName=Deprecated]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testManyAnnotationsOnFunction() {
        doBuildTest("[Deprecated Override] fun foo() {}",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=foo name=foo]\n" +
                    "    ANNOTATION_ENTRY:PsiJetAnnotationStubImpl[shortName=Deprecated]\n" +
                    "    ANNOTATION_ENTRY:PsiJetAnnotationStubImpl[shortName=Override]\n" +
                    "    VALUE_PARAMETER_LIST:PsiJetParameterListStubImpl\n");
    }

    public void testAnnotationOnLocalFunction() {
        doBuildTest("fun foo() { [Deprecated] fun innerFoo() {} }",
                    "PsiJetFileStubImpl[package=]\n" +
                    "  FUN:PsiJetFunctionStubImpl[top topFQName=foo name=foo]\n" +
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
