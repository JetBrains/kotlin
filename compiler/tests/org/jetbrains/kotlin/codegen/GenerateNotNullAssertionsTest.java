/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

public class GenerateNotNullAssertionsTest extends CodegenTestCase {
    @NotNull
    @Override
    protected String getPrefix() {
        return "notNullAssertions";
    }

    private void setUpEnvironment(boolean disableCallAssertions, boolean disableParamAssertions) {
        File[] extraClassPath = javaClassesOutputDirectory != null ? new File[] {javaClassesOutputDirectory} : new File[0];
        CompilerConfiguration configuration =
                KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, disableCallAssertions);
        configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, disableParamAssertions);

        myEnvironment = KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        myFiles = null;
    }

    private void loadSource(@NotNull String fileName) {
        loadFileByFullPath(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + getPrefix() + "/" + fileName);
    }

    private void compileJava(@NotNull String fileName) {
        javaClassesOutputDirectory = CodegenTestUtil.compileJava(
                Collections.singletonList(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + getPrefix() + "/" + fileName),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private void doTestCallAssertions(boolean disableCallAssertions) throws Exception {
        compileJava("A.java");
        setUpEnvironment(disableCallAssertions, true);

        loadSource("AssertionChecker.kt");
        generateFunction("checkAssertions").invoke(null, !disableCallAssertions);
    }

    public void testGenerateAssertions() throws Exception {
        doTestCallAssertions(false);
    }

    public void testDoNotGenerateAssertions() throws Exception {
        doTestCallAssertions(true);
    }

    public void testNoAssertionsForKotlinFromSource() throws Exception {
        setUpEnvironment(false, true);

        loadFiles(getPrefix() + "/noAssertionsForKotlin.kt", getPrefix() + "/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalledInMyClasses(true);
    }

    public void testNoAssertionsForKotlinFromBinary() throws Exception {
        setUpEnvironment(false, true);
        loadSource("noAssertionsForKotlin.kt");
        OutputFileCollection outputFiles = generateClassesInFile();
        javaClassesOutputDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        OutputUtilsKt.writeAllTo(outputFiles, javaClassesOutputDirectory);

        setUpEnvironment(false, true);
        loadSource("noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalledInMyClasses(false);
    }

    public void testGenerateParamAssertions() throws Exception {
        compileJava("doGenerateParamAssertions.java");
        setUpEnvironment(true, false);

        loadSource("doGenerateParamAssertions.kt");
        generateFunction().invoke(null);
    }

    public void testDoNotGenerateParamAssertions() throws Exception {
        setUpEnvironment(true, true);

        loadSource("doNotGenerateParamAssertions.kt");

        assertNoIntrinsicsMethodIsCalled("A", true);
    }

    public void testNoParamAssertionForPrivateMethod() throws Exception {
        setUpEnvironment(true, false);

        loadSource("noAssertionForPrivateMethod.kt");

        assertNoIntrinsicsMethodIsCalled("A", true);
    }

    public void testArrayListGet() {
        setUpEnvironment(false, false);

        loadSource("arrayListGet.kt");
        String text = generateToText();

        assertTrue(text.contains("checkExpressionValueIsNotNull"));
        assertTrue(text.contains("checkParameterIsNotNull"));
    }

    public void testJavaMultipleSubstitutions() {
        compileJava("javaMultipleSubstitutions.java");
        setUpEnvironment(false, false);

        loadSource("javaMultipleSubstitutions.kt");
        String text = generateToText();

        assertEquals(3, StringUtil.getOccurrenceCount(text, "checkExpressionValueIsNotNull"));
        assertEquals(3, StringUtil.getOccurrenceCount(text, "checkParameterIsNotNull"));
    }

    public void testAssertionForNotNullTypeParam() {
        setUpEnvironment(false, false);

        loadSource("assertionForNotNullTypeParam.kt");

        assertTrue(generateToText().contains("checkParameterIsNotNull"));
    }

    public void testNoAssertionForNullableGenericMethod() {
        setUpEnvironment(false, true);

        loadSource("noAssertionForNullableGenericMethod.kt");

        assertNoIntrinsicsMethodIsCalledInMyClasses(true);
    }

    public void testNoAssertionForNullableCaptured() {
        setUpEnvironment(false, true);

        loadSource("noAssertionForNullableCaptured.kt");

        assertNoIntrinsicsMethodIsCalledInMyClasses(true);
    }

    public void testAssertionForNotNullCaptured() {
        setUpEnvironment(false, true);

        loadSource("assertionForNotNullCaptured.kt");

        assertTrue(generateToText().contains("checkExpressionValueIsNotNull"));
    }

    public void testNoAssertionForNullableGenericMethodCall() {
        setUpEnvironment(false, true);

        loadSource("noAssertionForNullableGenericMethodCall.kt");

        assertNoIntrinsicsMethodIsCalled("A", true);
    }

    public void testParamAssertionMessage() throws Exception {
        setUpEnvironment(false, false);

        loadText("class A { fun foo(s: String) {} }");
        Class<?> a = generateClass("A");
        try {
            a.getDeclaredMethod("foo", String.class).invoke(a.newInstance(), new Object[] {null});
        }
        catch (InvocationTargetException ite) {
            Throwable e = ite.getTargetException();
            //noinspection ThrowableResultOfMethodCallIgnored
            assertInstanceOf(e, IllegalArgumentException.class);
            assertEquals("Parameter specified as non-null is null: method A.foo, parameter s", e.getMessage());
            return;
        }

        fail("Assertion should have been fired");
    }

    private void assertNoIntrinsicsMethodIsCalledInMyClasses(boolean noClassFileIsAnError) {
        for (KtFile jetFile : myFiles.getPsiFiles()) {
            String fileClassName = JvmFileClassUtil.getFileClassInfoNoResolve(jetFile).getFileClassFqName().asString();
            assertNoIntrinsicsMethodIsCalled(fileClassName, noClassFileIsAnError);
        }
    }

    private void assertNoIntrinsicsMethodIsCalled(String className, boolean noClassFileIsAnError) {
        OutputFileCollection classes = generateClassesInFile();
        OutputFile file = classes.get(className + ".class");
        if (noClassFileIsAnError) {
            assertNotNull("File for " + className + " is absent", file);
        }
        else if (file == null) {
            return;
        }
        ClassReader reader = new ClassReader(file.asByteArray());

        reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull String callerName, @NotNull String callerDesc, String signature, String[] exceptions
            ) {
                return new MethodVisitor(Opcodes.API_VERSION) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        assertFalse(
                                "Intrinsics method is called: " + name + desc + "  Caller: " + callerName + callerDesc,
                                "kotlin/jvm/internal/Intrinsics".equals(owner)
                        );
                    }
                };
            }
        }, 0);
    }
}
