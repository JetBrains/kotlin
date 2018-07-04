/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.jetbrains.kotlin.codegen.TestUtilsKt.clearReflectionCache;
import static org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getBoxMethodOrNull;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getGeneratedClass;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {

    @Override
    protected void doMultiFileTest(
        @NotNull File wholeFile,
        @NotNull List<TestFile> files,
        @Nullable File javaFilesDir
    ) throws Exception {
        try {
            compile(files, javaFilesDir);
            blackBox();
        }
        catch (Throwable t) {
            try {
                // To create .txt file in case of failure
                doBytecodeListingTest(wholeFile);
            }
            catch (Throwable ignored) {
            }

            throw t;
        }

        doBytecodeListingTest(wholeFile);
    }

    private void doBytecodeListingTest(@NotNull File wholeFile) throws Exception {
        if (!InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(wholeFile), "CHECK_BYTECODE_LISTING")) return;

        File expectedFile = new File(wholeFile.getParent(), FilesKt.getNameWithoutExtension(wholeFile) + ".txt");
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

    protected void blackBox() {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader();
        for (KtFile firstFile : myFiles.getPsiFiles()) {
            String className = getFacadeFqName(firstFile, classFileFactory.getGenerationState().getBindingContext());
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
                System.out.println(generateToText());
                throw ExceptionUtilsKt.rethrow(e);
            }
            finally {
                clearReflectionCache(generatedClassLoader);
            }
        }
        fail("Can't find box method!");
    }

    @Nullable
    private static String getFacadeFqName(@NotNull KtFile firstFile, @NotNull BindingContext bindingContext) {
        for (KtDeclaration declaration : CodegenUtil.getDeclarationsToGenerate(firstFile, bindingContext)) {
            if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction || declaration instanceof KtTypeAlias) {
                return JvmFileClassUtil.getFileClassInfoNoResolve(firstFile).getFacadeClassFqName().asString();
            }
        }
        return null;
    }
}
