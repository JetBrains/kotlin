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

package org.jetbrains.kotlin.codegen.flags;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.codegen.CodegenTestCase;
import org.jetbrains.org.objectweb.asm.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.findListWithPrefixes;
import static org.jetbrains.kotlin.test.InTextDirectivesUtils.findStringWithPrefixes;

/*
 * Test correctness of written flags in class file
 *
 *  TESTED_OBJECT_KIND - maybe class, function or property
 *  TESTED_OBJECTS - className, [function/property name]
 *  FLAGS - only flags which must be true (could be skipped if ABSENT is TRUE)
 *  ABSENT - true or false, optional (false by default)
 *
 * There is could be specified several tested objects separated by empty line, e.g:
 * TESTED_OBJECT_KIND: property
 * TESTED_OBJECTS: Test$object, prop
 * ABSENT: TRUE
 *
 * TESTED_OBJECT_KIND: property
 * TESTED_OBJECTS: Test, prop$delegate
 * FLAGS: ACC_STATIC, ACC_FINAL, ACC_PRIVATE
 */
public abstract class AbstractWriteFlagsTest extends CodegenTestCase {

    @Override
    protected void doMultiFileTest(
            @NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir
    ) throws Exception {
        compile(files, null);

        String fileText = FileUtil.loadFile(wholeFile, true);

        List<TestedObject> testedObjects = parseExpectedTestedObject(fileText);
        for (TestedObject testedObject : testedObjects) {
            String className = null;
            for (OutputFile outputFile : classFileFactory.asList()) {
                String filePath = outputFile.getRelativePath();
                if (testedObject.isFullContainingClassName && filePath.equals(testedObject.containingClass + ".class") ||
                    !testedObject.isFullContainingClassName && filePath.startsWith(testedObject.containingClass)) {
                    className = filePath;
                }
            }

            assertNotNull("Couldn't find a class file with name " + testedObject.containingClass, className);

            OutputFile outputFile = classFileFactory.get(className);
            assertNotNull(outputFile);

            ClassReader cr = new ClassReader(outputFile.asByteArray());
            TestClassVisitor classVisitor = getClassVisitor(testedObject.kind, testedObject.name, false);
            cr.accept(classVisitor, ClassReader.SKIP_CODE);

            if (!classVisitor.isExists())  {
                classVisitor = getClassVisitor(testedObject.kind, testedObject.name, true);
                cr.accept(classVisitor, ClassReader.SKIP_CODE);
            }

            boolean isObjectExists = !Boolean.valueOf(findStringWithPrefixes(testedObject.textData, "// ABSENT: "));
            assertEquals("Wrong object existence state: " + testedObject, isObjectExists, classVisitor.isExists());

            if (isObjectExists) {
                assertEquals("Wrong access flag for " + testedObject + " \n" + outputFile.asText(),
                             getExpectedFlags(testedObject.textData), classVisitor.getAccess());
            }
        }
    }

    private static List<TestedObject> parseExpectedTestedObject(String testDescription) {
        String[] testObjectData = testDescription.substring(testDescription.indexOf("// TESTED_OBJECT_KIND")).split("\n\n");
        List<TestedObject> objects = new ArrayList<>();

        for (String testData : testObjectData) {
            if (testData.isEmpty()) continue;

            TestedObject testObject = new TestedObject();
            testObject.textData = testData;
            List<String> testedObjects = findListWithPrefixes(testData, "// TESTED_OBJECTS: ");
            assertTrue("Cannot find TESTED_OBJECTS instruction", !testedObjects.isEmpty());
            testObject.containingClass = testedObjects.get(0);
            if (testedObjects.size() == 1) {
                testObject.name = testedObjects.get(0);
            }
            else if (testedObjects.size() == 2) {
                testObject.name = testedObjects.get(1);
            }
            else {
                throw new IllegalArgumentException(
                        "TESTED_OBJECTS instruction must contain one (for class) or two (for function and property) values");
            }

            testObject.kind = findStringWithPrefixes(testData, "// TESTED_OBJECT_KIND: ");
            List<String> isFullName = findListWithPrefixes(testData, "// IS_FULL_CONTAINING_CLASS_NAME: ");
            if (isFullName.size() == 1) {
                testObject.isFullContainingClassName = Boolean.parseBoolean(isFullName.get(0));
            }
            objects.add(testObject);
        }
        assertTrue("Test description not present!", !objects.isEmpty());
        return objects;
    }

    private static class TestedObject {
        public String name;
        public String containingClass = "";
        public boolean isFullContainingClassName = true;
        public String kind;
        public String textData;

        @Override
        public String toString() {
            return "Class = " + containingClass + ", name = " + name + ", kind = " + kind;
        }
    }

    private static TestClassVisitor getClassVisitor(String visitorKind, String testedObjectName, boolean allowSynthetic) {
        switch (visitorKind) {
            case "class":
                return new ClassFlagsVisitor();
            case "function":
                return new FunctionFlagsVisitor(testedObjectName, allowSynthetic);
            case "property":
                return new PropertyFlagsVisitor(testedObjectName);
            case "innerClass":
                return new InnerClassFlagsVisitor(testedObjectName);
                default:
        }

        throw new IllegalArgumentException("Value of TESTED_OBJECT_KIND is incorrect: " + visitorKind);
    }

    protected static abstract class TestClassVisitor extends ClassVisitor {

        protected boolean isExists;

        public TestClassVisitor() {
            super(Opcodes.ASM5);
        }

        abstract public int getAccess();

        public boolean isExists() {
            return isExists;
        }
    }

    private static int getExpectedFlags(String text) {
        int expectedAccess = 0;
        Class klass = Opcodes.class;
        List<String> flags = findListWithPrefixes(text, "// FLAGS: ");
        for (String flag : flags) {
            try {
                Field field = klass.getDeclaredField(flag);
                expectedAccess |= field.getInt(klass);
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot find " + flag + " field in Opcodes class", e);
            }
        }
        return expectedAccess;
    }

    private static class ClassFlagsVisitor extends TestClassVisitor {
        private int access = 0;

        @Override
        public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
            this.access = access;
            isExists = true;
        }

        @Override
        public int getAccess() {
            return access;
        }
    }

    private static class FunctionFlagsVisitor extends TestClassVisitor {
        private int access = 0;
        private final String funName;
        private final boolean allowSynthetic;

        public FunctionFlagsVisitor(String name, boolean allowSynthetic) {
            funName = name;
            this.allowSynthetic = allowSynthetic;
        }

        @Override
        public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions) {
            if (name.equals(funName)) {
                if (!allowSynthetic && (access & Opcodes.ACC_SYNTHETIC) != 0) return null;
                this.access = access;
                isExists = true;
            }
            return null;
        }

        @Override
        public int getAccess() {
            return access;
        }
    }

    private static class PropertyFlagsVisitor extends TestClassVisitor {
        private int access = 0;
        private final String propertyName;

        public PropertyFlagsVisitor(String name) {
            propertyName = name;
        }

        @Override
        public FieldVisitor visitField(int access, @NotNull String name, @NotNull String desc, String signature, Object value) {
            if (name.equals(propertyName)) {
                this.access = access;
                isExists = true;
            }
            return null;
        }

        @Override
        public int getAccess() {
            return access;
        }
    }

    private static class InnerClassFlagsVisitor extends TestClassVisitor {
        private int access = 0;
        private final String innerClassName;

        public InnerClassFlagsVisitor(String name) {
            innerClassName = name;
        }

        @Override
        public void visitInnerClass(@NotNull String innerClassInternalName, String outerClassInternalName, String name, int access) {
            if (innerClassName.equals(name)) {
                this.access = access;
                isExists = true;
            }
        }

        @Override
        public int getAccess() {
            return access;
        }
    }
}
