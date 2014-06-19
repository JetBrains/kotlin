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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class InnerClassInfoGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
        loadFile("innerClassInfo/" + getTestName(true) + ".kt");
    }

    public void testInnerClassInfo() {
        InnerClassAttribute innerB = new InnerClassAttribute("A$B", "A", "B", ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
        InnerClassAttribute innerC = new InnerClassAttribute("A$B$C", "A$B", "C", ACC_PUBLIC | ACC_FINAL);
        InnerClassAttribute innerAClassObject = new InnerClassAttribute(
                "A" + JvmAbi.CLASS_OBJECT_SUFFIX, "A", JvmAbi.CLASS_OBJECT_CLASS_NAME, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

        extractAndCompareInnerClasses("A", innerB, innerAClassObject);
        extractAndCompareInnerClasses("A$B", innerB, innerC);
        extractAndCompareInnerClasses("A$B$C", innerB, innerC);
        extractAndCompareInnerClasses("A" + JvmAbi.CLASS_OBJECT_SUFFIX, innerAClassObject);
    }

    public void testLocalClass() {
        InnerClassAttribute innerB = new InnerClassAttribute("A$foo$B", null, "B", ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

        extractAndCompareInnerClasses("A", innerB);
        extractAndCompareInnerClasses("A$foo$B", innerB);
    }

    public void testAnonymousClass() {
        InnerClassAttribute innerB = new InnerClassAttribute("A$B$1", null, null, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
        InnerClassAttribute innerC = new InnerClassAttribute("A$foo$C$1", null, null, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

        extractAndCompareInnerClasses("A", innerB, innerC);
        extractAndCompareInnerClasses("A$B$1", innerB);
        extractAndCompareInnerClasses("A$foo$C$1", innerC);
    }

    public void testAnonymousObjectInline() {
        InnerClassAttribute objectInInlineFun = new InnerClassAttribute("A$inlineFun$s$1", null, null, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
        extractAndCompareInnerClasses("A", objectInInlineFun);
    }

    public void testEnumEntry() {
        InnerClassAttribute innerE2 = new InnerClassAttribute("E$E2", "E", "E2", ACC_STATIC | ACC_FINAL);

        extractAndCompareInnerClasses("E", innerE2);
        extractAndCompareInnerClasses("E$E2", innerE2);
    }



    private void extractAndCompareInnerClasses(@NotNull String className, @NotNull InnerClassAttribute... expectedInnerClasses) {
        assertSameElements(extractInnerClasses(className), expectedInnerClasses);
    }

    @NotNull
    private List<InnerClassAttribute> extractInnerClasses(@NotNull String className) {
        OutputFile outputFile = generateClassesInFile().get(className + ".class");
        assertNotNull(outputFile);
        byte[] bytes = outputFile.asByteArray();
        ClassReader reader = new ClassReader(bytes);
        final List<InnerClassAttribute> result = new ArrayList<InnerClassAttribute>();

        reader.accept(new ClassVisitor(ASM5) {
            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                result.add(new InnerClassAttribute(name, outerName, innerName, access));
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

        return result;
    }

    private static class InnerClassAttribute {
        private final String name;
        @Nullable private final String outerName;
        @Nullable private final String innerName;
        private final int access;

        private InnerClassAttribute(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
            this.name = name;
            this.outerName = outerName;
            this.innerName = innerName;
            this.access = access;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InnerClassAttribute attribute = (InnerClassAttribute) o;

            if (!name.equals(attribute.name)) return false;
            if (outerName != null ? !outerName.equals(attribute.outerName) : attribute.outerName != null) return false;
            if (innerName != null ? !innerName.equals(attribute.innerName) : attribute.innerName != null) return false;
            if (access != attribute.access) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (outerName != null ? outerName.hashCode() : 0);
            result = 31 * result + (innerName != null ? innerName.hashCode() : 0);
            result = 31 * result + access;
            return result;
        }

        @Override
        public String toString() {
            return String.format("InnerClass(name=%s, outerName=%s, innerName=%s, access=%d)", name, outerName, innerName, access);
        }
    }
}
