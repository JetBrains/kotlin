/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
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
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;
import java.util.Optional;

// TODO Remove this class once general multi-module bytecode text tests are implemented.
public class SimplifiedMessagesOnDowncastTest extends CodegenTestCase {
    @NotNull
    @Override
    protected String getPrefix() {
        return "simplifyErrorMessagesForDowncast";
    }

    private void setUpEnvironment(boolean simplifyErrorsForDowncast) {
        File[] extraClassPath = javaClassesOutputDirectory != null ? new File[] {javaClassesOutputDirectory} : new File[0];
        CompilerConfiguration configuration =
                KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.SIMPLIFY_DOWNCAST_MESSAGES, simplifyErrorsForDowncast);
        myEnvironment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        myFiles = null;
    }

    private void loadSource(@NotNull String fileName) {
        loadFileByFullPath(KtTestUtil.getTestDataPathBase() + "/codegen/" + getPrefix() + "/" + fileName);
    }

    public void testExtendedErrorMessagesForDowncast() {
        boolean expectSimplified = true;
        // Simplified Errors Mode
        setUpEnvironment(expectSimplified);
        loadSource("simplifyErrorsForDowncast.kt");
        OutputFileCollection outputFiles = generateClassesInFile();
        javaClassesOutputDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        OutputUtilsKt.writeAllTo(outputFiles, javaClassesOutputDirectory);
        assertMessageIsSimplified("null cannot be cast to non-null type");
    }

    public void testSimplifiedErrorMessages() {
        boolean expectSimplified = false;
        // Simplified Errors Mode
        setUpEnvironment(expectSimplified);
        loadSource("simplifyErrorsForDowncast.kt");
        OutputFileCollection outputFiles = generateClassesInFile();
        javaClassesOutputDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        OutputUtilsKt.writeAllTo(outputFiles, javaClassesOutputDirectory);
        assertMessageIsSimplified("null cannot be cast to non-null type foo.A");
    }

    private void assertMessageIsSimplified(String expectedErrorMessage) {
        OutputFileCollection classes = generateClassesInFile();
        // There should only be a single class "A.class in a package foo"
        OutputFile file = classes.get("foo/A.class");
        if (file == null) {
            fail("Expected an output class");
        }
        ClassReader reader = new ClassReader(file.asByteArray());
        String[] errorMessage = new String[1]; // By ref
        reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull String callerName, @NotNull String callerDesc, String signature, String[] exceptions
            ) {
                if (callerName.equals("printName")) {
                    return new MethodVisitor(Opcodes.API_VERSION) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            errorMessage[0] = value.toString();
                        }
                    };
                }
                else {
                    return super.visitMethod(access, callerName, callerDesc, signature, exceptions);
                }
            }
        }, 0);
        assertEquals(errorMessage[0], expectedErrorMessage);
    }
}
