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

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public abstract class AbstractScriptCodegenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected void doTest(@NotNull String filename) {
        loadFileByFullPath(filename);

        try {
            //noinspection ConstantConditions
            FqName fqName = myFiles.getPsiFile().getScript().getFqName();
            Class<?> scriptClass = generateClass(fqName.asString());

            Constructor constructor = getTheOnlyConstructor(scriptClass);
            Object scriptInstance = constructor.newInstance(myFiles.getScriptParameterValues().toArray());

            assertFalse("expecting at least one expectation", myFiles.getExpectedValues().isEmpty());

            for (Pair<String, String> nameValue : myFiles.getExpectedValues()) {
                String fieldName = nameValue.first;
                String expectedValue = nameValue.second;

                if (expectedValue.equals("<nofield>")) {
                    try {
                        scriptClass.getDeclaredField(fieldName);
                        fail("must have no field " + fieldName);
                    }
                    catch (NoSuchFieldException e) {
                        continue;
                    }
                }

                Field field = scriptClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object result = field.get(scriptInstance);
                String resultString = result != null ? result.toString() : "null";
                assertEquals("comparing field " + fieldName, expectedValue, resultString);
            }
        }
        catch (Throwable e) {
            System.out.println(generateToText());
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    private static Constructor getTheOnlyConstructor(@NotNull Class<?> clazz) {
        Constructor[] constructors = clazz.getConstructors();
        if (constructors.length != 1) {
            throw new IllegalArgumentException("Script class should have one constructor: " + clazz);
        }
        return constructors[0];
    }
}
