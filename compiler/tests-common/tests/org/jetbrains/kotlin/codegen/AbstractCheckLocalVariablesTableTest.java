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

import com.google.common.io.Files;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test correctness of written local variables in class file for specified method
 */

public abstract class AbstractCheckLocalVariablesTableTest extends TestCaseWithTmpdir {
    private File ktFile;
    private KotlinCoreEnvironment environment;

    public AbstractCheckLocalVariablesTableTest() {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        environment = KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable);
    }

    @Override
    protected void tearDown() throws Exception {
        environment = null;
        super.tearDown();
    }

    protected void doTest(@NotNull String ktFileName) throws Exception {
        ktFile = new File(ktFileName);
        String text = FileUtil.loadFile(ktFile, true);

        KtFile psiFile = KotlinTestUtils.createFile(ktFile.getName(), text, environment.getProject());

        OutputFileCollection outputFiles = GenerationUtils.compileFile(psiFile, environment);

        String classAndMethod = parseClassAndMethodSignature();
        String[] split = classAndMethod.split("\\.");
        assert split.length == 2 : "Exactly one dot is expected: " + classAndMethod;
        String classFileRegex = StringUtil.escapeToRegexp(split[0] + ".class").replace("\\*", ".+");
        String methodName = split[1];

        OutputFile outputFile = ContainerUtil.find(outputFiles.asList(), file -> file.getRelativePath().matches(classFileRegex));

        String pathsString = StringUtil.join(outputFiles.asList(), OutputFile::getRelativePath, ", ");
        assertNotNull("Couldn't find class file for pattern " + classFileRegex + " in: " + pathsString, outputFile);

        ClassReader cr = new ClassReader(outputFile.asByteArray());
        List<LocalVariable> actualLocalVariables = readLocalVariable(cr, methodName);

        KotlinTestUtils.assertEqualsToFile(ktFile, text.substring(0, text.indexOf("// VARIABLE : ")) + getActualVariablesAsString(actualLocalVariables));
    }

    private static String getActualVariablesAsString(List<LocalVariable> list) {
        StringBuilder builder = new StringBuilder();
        for (LocalVariable variable : list) {
            builder.append(variable.toString()).append("\n");
        }
        return builder.toString();
    }


    private static class LocalVariable {
        private final String name;
        private final String type;
        private final int index;

        private LocalVariable(
                @NotNull String name,
                @NotNull String type,
                int index
        ) {
            this.name = name;
            this.type = type;
            this.index = index;
        }

        @Override
        public String toString() {
            return "// VARIABLE : NAME=" + name + " TYPE=" + type + " INDEX=" + index;
        }
    }

    @NotNull
    private static final Pattern methodPattern = Pattern.compile("^// METHOD : *(.*)");

    @NotNull
    private String parseClassAndMethodSignature() throws IOException {
        List<String> lines = Files.readLines(ktFile, Charset.forName("utf-8"));
        for (String line : lines) {
            Matcher methodMatcher = methodPattern.matcher(line);
            if (methodMatcher.matches()) {
                return methodMatcher.group(1);
            }
        }

        throw new AssertionError("method instructions not found");
    }

    @NotNull
    private static List<LocalVariable> readLocalVariable(ClassReader cr, String methodName) throws Exception {
        class Visitor extends ClassVisitor {
            List<LocalVariable> readVariables = new ArrayList<>();

            public Visitor() {
                super(Opcodes.ASM5);
            }

            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions
            ) {
                if (methodName.equals(name + desc)) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitLocalVariable(
                                @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end, int index
                        ) {
                            readVariables.add(new LocalVariable(name, desc, index));
                        }
                    };
                }
                else {
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            }
        }
        Visitor visitor = new Visitor();

        cr.accept(visitor, ClassReader.SKIP_FRAMES);

        assertFalse("method not found: " + methodName, visitor.readVariables.size() == 0);

        return visitor.readVariables;
    }
}

