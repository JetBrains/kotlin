/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.jetbrains.kotlin.codegen.TestUtilsKt.*;
import static org.jetbrains.kotlin.test.KotlinTestUtils.assertEqualsToFile;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getBoxMethodOrNull;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getGeneratedClass;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {

    @Override
    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir) throws Exception {
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

        assertEqualsToFile(expectedFile, text);
    }

    protected void blackBox() {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader();
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
    private static String getFacadeFqName(@NotNull KtFile firstFile) {
        for (KtDeclaration declaration : firstFile.getDeclarations()) {
            if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction) {
                return JvmFileClassUtil.getFileClassInfoNoResolve(firstFile).getFacadeClassFqName().asString();
            }
        }
        return null;
    }
}
