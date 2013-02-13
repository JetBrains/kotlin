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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import jet.runtime.Intrinsics;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.File;

import static org.jetbrains.jet.codegen.CodegenTestUtil.compileJava;

public class GenerateNotNullAssertionsTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    private void setUpEnvironment(boolean generateAssertions, boolean generateParamAssertions, File... extraClassPath) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, generateAssertions);
        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, generateParamAssertions);

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), configuration);
    }

    private void doTestGenerateAssertions(boolean generateAssertions) throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/A.java");

        setUpEnvironment(generateAssertions, false, javaClassesTempDirectory);

        loadFile("notNullAssertions/AssertionChecker.kt");
        generateFunction("checkAssertions").invoke(null, generateAssertions);
    }

    public void testGenerateAssertions() throws Exception {
        doTestGenerateAssertions(true);
    }

    public void testDoNotGenerateAssertions() throws Exception {
        doTestGenerateAssertions(false);
    }

    public void testNoAssertionsForKotlinFromSource() throws Exception {
        setUpEnvironment(true, false);

        loadFiles("notNullAssertions/noAssertionsForKotlin.kt", "notNullAssertions/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalled(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    public void testNoAssertionsForKotlinFromBinary() throws Exception {
        setUpEnvironment(true, false);
        loadFile("notNullAssertions/noAssertionsForKotlin.kt");
        ClassFileFactory factory = generateClassesInFile();
        File compiledDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        CompileEnvironmentUtil.writeToOutputDirectory(factory, compiledDirectory);

        setUpEnvironment(true, false, compiledDirectory);
        loadFile("notNullAssertions/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalled(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    public void testGenerateParamAssertions() throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/doGenerateParamAssertions.java");

        setUpEnvironment(false, true, javaClassesTempDirectory);

        loadFile("notNullAssertions/doGenerateParamAssertions.kt");
        generateFunction().invoke(null);
    }

    public void testDoNotGenerateParamAssertions() throws Exception {
        setUpEnvironment(false, false);

        loadFile("notNullAssertions/doNotGenerateParamAssertions.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    public void testNoParamAssertionForPrivateMethod() throws Exception {
        setUpEnvironment(false, true);

        loadFile("notNullAssertions/noAssertionForPrivateMethod.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    public void testArrayListGet() {
        setUpEnvironment(true, true);

        loadFile("notNullAssertions/arrayListGet.kt");
        String text = generateToText();

        assertTrue(text.contains("checkReturnedValueIsNotNull"));
        assertTrue(text.contains("checkParameterIsNotNull"));
    }

    public void testJavaMultipleSubstitutions() {
        File javaClassesTempDirectory = compileJava("notNullAssertions/javaMultipleSubstitutions.java");
        setUpEnvironment(true, true, javaClassesTempDirectory);

        loadFile("notNullAssertions/javaMultipleSubstitutions.kt");
        String text = generateToText();

        assertEquals(3, StringUtil.getOccurrenceCount(text, "checkReturnedValueIsNotNull"));
        assertEquals(3, StringUtil.getOccurrenceCount(text, "checkParameterIsNotNull"));
    }

    public void testAssertionForNotNullTypeParam() {
        setUpEnvironment(true, true);

        loadFile("notNullAssertions/assertionForNotNullTypeParam.kt");

        assertTrue(generateToText().contains("checkParameterIsNotNull"));
    }

    public void testNoAssertionForNullableGenericMethod() {
        setUpEnvironment(true, false);

        loadFile("notNullAssertions/noAssertionForNullableGenericMethod.kt");

        assertNoIntrinsicsMethodIsCalled(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    public void testNoAssertionForNullableGenericMethodCall() {
        setUpEnvironment(true, false);

        loadFile("notNullAssertions/noAssertionForNullableGenericMethodCall.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    private void assertNoIntrinsicsMethodIsCalled(String className) {
        ClassFileFactory classes = generateClassesInFile();
        ClassReader reader = new ClassReader(classes.asBytes(className + ".class"));

        final String intrinsics = JvmClassName.byFqNameWithoutInnerClasses(Intrinsics.class.getName()).getInternalName();

        reader.accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(
                    int access, final String callerName, final String callerDesc, String signature, String[] exceptions
            ) {
                return new MethodVisitor(Opcodes.ASM4) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        assertFalse(
                                "Intrinsics method is called: " + name + desc + "  Caller: " + callerName + callerDesc,
                                intrinsics.equals(owner)
                        );
                    }
                };
            }
        }, 0);
    }
}
