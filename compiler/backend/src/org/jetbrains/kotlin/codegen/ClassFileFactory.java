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

import com.google.common.collect.Lists;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class ClassFileFactory implements OutputFileCollection {
    private final GenerationState state;
    private final ClassBuilderFactory builderFactory;
    private final Map<FqName, PackageCodegen> package2codegen = new HashMap<FqName, PackageCodegen>();
    private final Map<String, OutAndSourceFileList> generators = new LinkedHashMap<String, OutAndSourceFileList>();

    private boolean isDone = false;

    public ClassFileFactory(@NotNull GenerationState state, @NotNull ClassBuilderFactory builderFactory) {
        this.state = state;
        this.builderFactory = builderFactory;
    }

    @NotNull
    public ClassBuilder newVisitor(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull Type asmType,
            @NotNull PsiFile sourceFile) {
        return newVisitor(origin, asmType, Collections.singletonList(sourceFile));
    }

    @NotNull
    public ClassBuilder newVisitor(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull Type asmType,
            @NotNull Collection<? extends PsiFile> sourceFiles) {
        String outputFilePath = asmType.getInternalName() + ".class";
        List<File> ioSourceFiles = toIoFilesIgnoringNonPhysical(sourceFiles);
        state.getProgress().reportOutput(ioSourceFiles, new File(outputFilePath));
        ClassBuilder answer = builderFactory.newClassBuilder(origin);
        generators.put(outputFilePath, new ClassBuilderAndSourceFileList(answer, ioSourceFiles));
        return answer;
    }

    void done() {
        if (!isDone) {
            isDone = true;
            Collection<PackageCodegen> values = package2codegen.values();
            for (PackageCodegen codegen : values) {
                codegen.done();
            }

            writeModuleMappings(values);
        }
    }

    private void writeModuleMappings(Collection<PackageCodegen> values) {
        String moduleName = KotlinPackage.removeSurrounding(state.getModule().getName().asString(), "<", ">");
        String outputFilePath = "META-INF/" + moduleName + ".kotlin_module";
        final StringWriter moduleMapping = new StringWriter(1024);
        for (PackageCodegen codegen : values) {
            codegen.getFacades().serialize(moduleMapping);
        }
        state.getProgress().reportOutput(Collections.<File>emptyList(), new File(outputFilePath));
        //TODO: source files?
        generators.put(outputFilePath, new OutAndSourceFileList(Collections.<File>emptyList()) {
            @Override
            public byte[] asBytes(ClassBuilderFactory factory) {
                try {
                    return moduleMapping.toString().getBytes("UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String asText(ClassBuilderFactory factory) {
                return moduleMapping.toString();
            }
        });
    }

    @NotNull
    @Override
    public List<OutputFile> asList() {
        done();
        return ContainerUtil.map(generators.keySet(), new Function<String, OutputFile>() {
            @Override
            public OutputFile fun(String relativeClassFilePath) {
                return new OutputClassFile(relativeClassFilePath);
            }
        });
    }

    @Override
    @Nullable
    public OutputFile get(@NotNull String relativePath) {
        return generators.containsKey(relativePath) ? new OutputClassFile(relativePath) : null;
    }

    @NotNull
    @TestOnly
    public String createText() {
        StringBuilder answer = new StringBuilder();

        for (OutputFile file : asList()) {
            answer.append("@").append(file.getRelativePath()).append('\n');
            answer.append(file.asText());
        }

        return answer.toString();
    }

    @NotNull
    public PackageCodegen forPackage(@NotNull FqName fqName, @NotNull Collection<JetFile> files) {
        assert !isDone : "Already done!";
        PackageCodegen codegen = package2codegen.get(fqName);
        if (codegen == null) {
            codegen = new PackageCodegen(state, files, fqName);
            package2codegen.put(fqName, codegen);
        }

        return codegen;
    }

    @NotNull
    public PackageCodegen forFacade(@NotNull FqName facadeFqName, @NotNull Collection<JetFile> files) {
        assert !isDone : "Already done!";
        PackageCodegen codegen = package2codegen.get(facadeFqName);
        if (codegen == null) {
            codegen = new PackageCodegen(state, files, facadeFqName.parent());
            package2codegen.put(facadeFqName, codegen);
        }

        return codegen;
    }

    @NotNull
    private static List<File> toIoFilesIgnoringNonPhysical(@NotNull Collection<? extends PsiFile> psiFiles) {
        List<File> result = Lists.newArrayList();
        for (PsiFile psiFile : psiFiles) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            // We ignore non-physical files here, because this code is needed to tell the make what inputs affect which outputs
            // a non-physical file cannot be processed by make
            if (virtualFile != null) {
                result.add(new File(virtualFile.getPath()));
            }
        }
        return result;
    }

    private class OutputClassFile implements OutputFile {
        private final String relativeClassFilePath;

        public OutputClassFile(String relativeClassFilePath) {
            this.relativeClassFilePath = relativeClassFilePath;
        }

        @NotNull
        @Override
        public String getRelativePath() {
            return relativeClassFilePath;
        }

        @NotNull
        @Override
        public List<File> getSourceFiles() {
            OutAndSourceFileList pair = generators.get(relativeClassFilePath);
            if (pair == null) {
                throw new IllegalStateException("No record for binary file " + relativeClassFilePath);
            }

            return pair.sourceFiles;
        }

        @NotNull
        @Override
        public byte[] asByteArray() {
            return generators.get(relativeClassFilePath).asBytes(builderFactory);
        }

        @NotNull
        @Override
        public String asText() {
            return generators.get(relativeClassFilePath).asText(builderFactory);
        }

        @NotNull
        @Override
        public String toString() {
            return getRelativePath() + " (compiled from " + getSourceFiles() + ")";
        }
    }

    private static final class ClassBuilderAndSourceFileList extends OutAndSourceFileList {
        private final ClassBuilder classBuilder;

        private ClassBuilderAndSourceFileList(ClassBuilder classBuilder, List<File> sourceFiles) {
            super(sourceFiles);
            this.classBuilder = classBuilder;
        }

        @Override
        public byte[] asBytes(ClassBuilderFactory factory) {
            return factory.asBytes(classBuilder);
        }

        @Override
        public String asText(ClassBuilderFactory factory) {
            return factory.asText(classBuilder);
        }
    }

    private static abstract class OutAndSourceFileList {

        protected final List<File> sourceFiles;

        private OutAndSourceFileList(List<File> sourceFiles) {
            this.sourceFiles = sourceFiles;
        }

        public abstract byte[] asBytes(ClassBuilderFactory factory);

        public abstract String asText(ClassBuilderFactory factory);
    }

    public void removeInlinedClasses(Set<String> classNamesToRemove) {
        for (String classInternalName : classNamesToRemove) {
            generators.remove(classInternalName + ".class");
        }
    }

    @TestOnly
    public List<JetFile> getInputFiles() {
        return state.getFiles();
    }
}
