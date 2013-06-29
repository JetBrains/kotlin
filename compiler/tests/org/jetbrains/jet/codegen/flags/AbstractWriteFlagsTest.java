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

package org.jetbrains.jet.codegen.flags;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.asm4.*;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.lang.psi.JetFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.InTextDirectivesUtils.findListWithPrefixes;
import static org.jetbrains.jet.InTextDirectivesUtils.findStringWithPrefixes;

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
public abstract class AbstractWriteFlagsTest extends UsefulTestCase {

    private JetCoreEnvironment jetCoreEnvironment;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected void tearDown() throws Exception {
        jetCoreEnvironment = null;
        super.tearDown();
    }

    protected void doTest(String path) throws IOException {
        File ktFile = new File(path);
        assertTrue("Cannot find a file " + ktFile.getAbsolutePath(), ktFile.exists());

        String fileText = FileUtil.loadFile(ktFile, true);

        JetFile psiFile = JetTestUtils.createFile(ktFile.getName(), fileText, jetCoreEnvironment.getProject());
        assertTrue("Cannot create JetFile from text", psiFile != null);

        ClassFileFactory factory = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile);

        List<TestedObject> testedObjects = parseExpectedTestedObject(fileText);
        for (TestedObject testedObject : testedObjects) {
            String className = null;
            for (String filename : factory.files()) {
                if (testedObject.isFullContainingClassName && filename.equals(testedObject.containingClass + ".class")) {
                    className = filename;
                }
                else if (!testedObject.isFullContainingClassName && filename.startsWith(testedObject.containingClass)) {
                    className = filename;
                }
            }

            if (className == null) {
                throw new AssertionError("Couldn't find a class file with name " + testedObject.containingClass);
            }

            ClassReader cr = new ClassReader(factory.asBytes(className));
            TestClassVisitor classVisitor;
            classVisitor = getClassVisitor(testedObject.kind, testedObject.name);
            cr.accept(classVisitor, ClassReader.SKIP_CODE);

            boolean isObjectExists = false == Boolean.valueOf(findStringWithPrefixes(testedObject.textData, "// ABSENT: "));
            assertEquals( "Wrong object existence state: " + testedObject, isObjectExists, classVisitor.isExists());
            int expectedAccess = getExpectedFlags(testedObject.textData);

            if (isObjectExists) {
                assertEquals("Wrong access flag for " + testedObject + " \n" + factory.asText(className), expectedAccess, classVisitor.getAccess());
            }
        }
    }

    private static List<TestedObject> parseExpectedTestedObject(String testDescription) {
        testDescription = testDescription.substring(testDescription.indexOf("// TESTED_OBJECT_KIND"));
        String [] testObjectData = testDescription.split("\n\n");
        ArrayList<TestedObject> objects = new ArrayList<TestedObject>();

        for (int i = 0; i < testObjectData.length; i++) {
            String testData = testObjectData[i];
            if (!testData.isEmpty()) {
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
                            "TESTED_OBJECTS instruction must contains one (for class) or two (for function and property) values");
                }

                testObject.kind = findStringWithPrefixes(testData, "// TESTED_OBJECT_KIND: ");
                List<String> isFullName = findListWithPrefixes(testData, "// IS_FULL_CONTAINING_CLASS_NAME: ");
                if (isFullName.size() == 1) {
                    testObject.isFullContainingClassName = Boolean.parseBoolean(isFullName.get(0));
                }
                objects.add(testObject);
            }
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

    private static TestClassVisitor getClassVisitor(String visitorKind, String testedObjectName) {
        if (visitorKind.equals("class")) {
            return new ClassFlagsVisitor();
        }
        else if (visitorKind.equals("function")) {
            return new FunctionFlagsVisitor(testedObjectName);
        }
        else if (visitorKind.equals("property")) {
            return new PropertyFlagsVisitor(testedObjectName);
        }
        else if (visitorKind.equals("innerClass")) {
            return new InnerClassFlagsVisitor(testedObjectName);
        }

        throw new IllegalArgumentException("Value of TESTED_OBJECT_KIND is incorrect: " + visitorKind);
    }

    protected static abstract class TestClassVisitor extends ClassVisitor {

        protected boolean isExists;

        public TestClassVisitor() {
            super(Opcodes.ASM4);
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
            catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Cannot find " + flag + " field in Opcodes class", e);
            }
            catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot find " + flag + " field in Opcodes class", e);
            }
        }
        return expectedAccess;
    }

    private static class ClassFlagsVisitor extends TestClassVisitor {
        private int access = 0;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
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

        public FunctionFlagsVisitor(String name) {
            funName = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (name.equals(funName)) {
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
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
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
        public void visitInnerClass(String innerClassInternalName, String outerClassInternalName, String name, int access) {
            if (name.equals(innerClassName)) {
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
