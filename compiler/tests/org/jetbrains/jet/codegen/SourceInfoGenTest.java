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

import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.ConfigurationKind;

public class SourceInfoGenTest extends CodegenTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testSingleFileNamespace() {
        String producer = "sourceInfo/foo1.kt";
        loadFiles(producer);
        assertEquals(producer, getProducerInfo("foo/FooPackage.class"));
    }

    public void testMultiFileNamespace() {
        loadFiles("sourceInfo/foo1.kt", "sourceInfo/foo2.kt");
        assertEquals(null, getProducerInfo("foo/FooPackage.class"));
    }

    public void testSingleClass() {
        String producer = "sourceInfo/singleClass.kt";
        loadFiles(producer);
        assertEquals(producer, getProducerInfo("SingleClass.class"));
    }

    private String getProducerInfo(String name) {
        ClassReader classReader = new ClassReader(generateClassesInFile().asBytes(name));

        final String [] producer = new String [1];
        classReader.accept(new ClassVisitor(Opcodes.ASM4) {

            @Override
            public void visitSource(String source, String debug) {
                producer[0] = source;
            }

        }, 0);
        return producer[0];
    }
}
