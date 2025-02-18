/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.ObsoleteTestInfrastructure;
import org.jetbrains.kotlin.TestsRuntimeError;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.jetbrains.kotlin.codegen.CodegenTestUtilsKt.getBoxMethodOrNull;
import static org.jetbrains.kotlin.codegen.CodegenTestUtilsKt.getGeneratedClass;

@ObsoleteTestInfrastructure(replacer = "org.jetbrains.kotlin.test.runners.codegen.AbstractBlackBoxCodegenTest")
public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {
    protected void doMultiFileTest(
            @NotNull File wholeFile,
            @NotNull List<TestFile> files,
            boolean unexpectedBehaviour
    ) throws Exception {
        boolean isIgnored = isIgnoredTarget(wholeFile);

        compile(files, !isIgnored, false);

        try {
            blackBox(!isIgnored, unexpectedBehaviour);
        }
        catch (Throwable t) {
            throw new TestsRuntimeError(t);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doMultiFileTest(
        @NotNull File wholeFile,
        @NotNull List<? extends TestFile> files
    ) throws Exception {
        doMultiFileTest(wholeFile, (List<TestFile>) files, false);
    }

    private void blackBox(boolean reportProblems, boolean unexpectedBehaviour) {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader(reportProblems);
        for (KtFile firstFile : myFiles.getPsiFiles()) {
            String className = getFacadeFqName(firstFile);
            if (className == null) continue;
            Class<?> aClass = getGeneratedClass(generatedClassLoader, className);
            try {
                Method method = getBoxMethodOrNull(aClass);
                if (method != null) {
                    callBoxMethodAndCheckResult(generatedClassLoader, aClass, method, unexpectedBehaviour);
                    return;
                }
            }
            catch (Throwable e) {
                if (reportProblems) {
                    System.out.println(generateToText());
                }
                throw ExceptionUtilsKt.rethrow(e);
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

    protected boolean isIgnoredTarget(@NotNull File wholeFile) {
        return InTextDirectivesUtils.isIgnoredTarget(getBackend(), wholeFile);
    }
}
