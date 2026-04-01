/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.FirParser;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

public class SourceInfoGenTest extends CodegenTestCase {
    @Override
    public boolean getUseFir() {
        return true;
    }

    @Override
    public @NotNull FirParser getFirParser() {
        return FirParser.LightTree;
    }

    private static final String TEST_FOLDER = "sourceInfo/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testSingleClass() {
        String producer = "singleClass.kt";
        loadFiles(TEST_FOLDER + producer);
        assertEquals(producer, getProducerInfo("SingleClass.class"));
    }

    private String getProducerInfo(String name) {
        OutputFile file = generateClassesInFile().get(name);
        assertNotNull(file);

        ClassReader classReader = new ClassReader(file.asByteArray());

        String[] producer = new String[1];
        classReader.accept(new ClassVisitor(Opcodes.API_VERSION) {

            @Override
            public void visitSource(String source, String debug) {
                producer[0] = source;
            }
        }, 0);
        return producer[0];
    }
}
