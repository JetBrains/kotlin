/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.TestsRuntimeError;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.jetbrains.kotlin.codegen.TestUtilsKt.clearReflectionCache;
import static org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getBoxMethodOrNull;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getGeneratedClass;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {

    private static final boolean IGNORE_EXPECTED_FAILURES =
            Boolean.getBoolean("kotlin.suppress.expected.test.failures");

    @Override
    protected void doMultiFileTest(
        @NotNull File wholeFile,
        @NotNull List<TestFile> files,
        @Nullable File javaFilesDir
    ) throws Exception {
        boolean isIgnored = IGNORE_EXPECTED_FAILURES && InTextDirectivesUtils.isIgnoredTarget(getBackend(), wholeFile);

        compile(files, javaFilesDir, !isIgnored);

        try {
            blackBox(!isIgnored);
        }
        catch (Throwable t) {
            if (!isIgnored) {
                try {
                    // To create .txt file in case of failure
                    doBytecodeListingTest(wholeFile);
                }
                catch (Throwable ignored) {
                }
            }

            throw new TestsRuntimeError(t);
        }

        doBytecodeListingTest(wholeFile);
    }

    private void doBytecodeListingTest(@NotNull File wholeFile) throws Exception {
        if (!InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(wholeFile), "CHECK_BYTECODE_LISTING")) return;

        String suffix =
                (coroutinesPackage.contains("experimental") || coroutinesPackage.isEmpty())
                && InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(wholeFile), "COMMON_COROUTINES_TEST")
                ? "_1_2" : "";
        File expectedFile = new File(wholeFile.getParent(), FilesKt.getNameWithoutExtension(wholeFile) + suffix + ".txt");

        String text =
                BytecodeListingTextCollectingVisitor.Companion.getText(
                        classFileFactory,
                        new BytecodeListingTextCollectingVisitor.Filter() {
                            @Override
                            public boolean shouldWriteClass(int access, @NotNull String name) {
                                return !name.startsWith("helpers/");
                            }

                            @Override
                            public boolean shouldWriteMethod(int access, @NotNull String name, @NotNull String desc) {
                                return true;
                            }

                            @Override
                            public boolean shouldWriteField(int access, @NotNull String name, @NotNull String desc) {
                                return true;
                            }

                            @Override
                            public boolean shouldWriteInnerClass(@NotNull String name) {
                                return true;
                            }
                        }
                );

        assertEqualsToFile(expectedFile, text, s -> s.replace("COROUTINES_PACKAGE", coroutinesPackage));
    }

    protected void blackBox(boolean reportProblems) {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader(reportProblems);
        for (KtFile firstFile : myFiles.getPsiFiles()) {
            String className = getFacadeFqName(firstFile);
            if (className == null) continue;
            Class<?> aClass = getGeneratedClass(generatedClassLoader, className);
            try {
                Method method = getBoxMethodOrNull(aClass);
                if (method != null) {
                    callBoxMethodAndCheckResult(generatedClassLoader, aClass, method);
                    return;
                }
            }
            catch (Throwable e) {
                if (reportProblems) {
                    System.out.println(generateToText());
                }
                throw ExceptionUtilsKt.rethrow(e);
            }
            finally {
                clearReflectionCache(generatedClassLoader);
            }
        }
        fail("Can't find box method!");
    }

    @Nullable
    private static String getFacadeFqName(@NotNull KtFile file) {
        return CodegenUtil.getMemberDeclarationsToGenerate(file).isEmpty()
               ? null
               : JvmFileClassUtil.getFileClassInfoNoResolve(file).getFacadeClassFqName().asString();
    }

    protected TargetBackend getBackend() {
        return TargetBackend.JVM;
    }
}
