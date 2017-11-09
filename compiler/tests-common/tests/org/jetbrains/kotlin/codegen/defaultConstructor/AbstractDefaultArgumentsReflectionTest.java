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

package org.jetbrains.kotlin.codegen.defaultConstructor;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.kotlin.codegen.CodegenTestCase;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.findListWithPrefixes;

public abstract class AbstractDefaultArgumentsReflectionTest extends CodegenTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected void doTest(String path) throws IOException {
        loadFileByFullPath(path);

        String fileText = FileUtil.loadFile(new File(path), true);
        String className = loadInstructionValue(fileText, "CLASS");
        boolean hasDefaultConstructor = loadInstructionValue(fileText, "HAS_DEFAULT_CONSTRUCTOR").equals("true");

        Class<?> aClass = generateClass(className);
        assertNotNull("Cannot find class with name " + className, aClass);
        try {
            Constructor constructor = aClass.getDeclaredConstructor();
            if (!hasDefaultConstructor) {
                System.out.println(generateToText());
                throw new AssertionError("Default constructor was found but it wasn't expected: " + constructor);
            }
        }
        catch (NoSuchMethodException e) {
            if (hasDefaultConstructor) {
                System.out.println(generateToText());
                throw new AssertionError("Cannot find default constructor");
            }
        }
        catch (Throwable e) {
            System.out.println(generateToText());
            throw new RuntimeException(e);
        }
    }

    private static String loadInstructionValue(String fileContent, String instructionName) {
        List<String> testedClass = findListWithPrefixes(fileContent, "// " + instructionName + ": ");
        assertTrue("Cannot find " + instructionName + " instruction", !testedClass.isEmpty());
        assertTrue(instructionName + " instruction must have only one element", testedClass.size() == 1);
        return testedClass.get(0);
    }
}
