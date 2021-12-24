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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.JvmAnalysisFlags;
import org.jetbrains.kotlin.load.kotlin.ModuleMappingUtilKt;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMappingKt;
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.serialization.StringTableImpl;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.getMappingFileName;

public class ClassFileFactory implements OutputFileCollection {
    private final GenerationState state;
    private final ClassBuilderFactory builderFactory;
    private final List<ClassFileFactoryFinalizerExtension> finalizers;
    private final Map<String, OutAndSourceFileList> generators = Collections.synchronizedMap(new LinkedHashMap<>());

    private boolean isDone = false;

    private final Set<File> sourceFiles = new HashSet<>();
    private final PackagePartRegistry packagePartRegistry = new PackagePartRegistry();

    public ClassFileFactory(@NotNull GenerationState state, @NotNull ClassBuilderFactory builderFactory, @NotNull List<ClassFileFactoryFinalizerExtension> finalizers) {
        this.state = state;
        this.builderFactory = builderFactory;
        this.finalizers = finalizers;
    }

    public GenerationState getGenerationState() {
        return state;
    }

    @NotNull
    public PackagePartRegistry getPackagePartRegistry() {
        return packagePartRegistry;
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
            @NotNull Collection<? extends PsiFile> sourceFiles
    ) {
        ClassBuilder answer = builderFactory.newClassBuilder(origin);
        generators.put(
                asmType.getInternalName() + ".class",
                new ClassBuilderAndSourceFileList(answer, toIoFilesIgnoringNonPhysical(sourceFiles))
        );
        return answer;
    }

    @NotNull
    public ClassBuilder newVisitor(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull Type asmType,
            @NotNull List<File> sourceFiles
    ) {
        ClassBuilder answer = builderFactory.newClassBuilder(origin);
        generators.put(
                asmType.getInternalName() + ".class",
                new ClassBuilderAndSourceFileList(answer, sourceFiles)
        );
        return answer;
    }

    public void done() {
        if (!isDone) {
            isDone = true;
            writeModuleMappings();
            for (ClassFileFactoryFinalizerExtension extension : finalizers) {
                extension.finalizeClassFactory(this);
            }
        }
    }

    public void releaseGeneratedOutput() {
        generators.clear();
    }

    private void writeModuleMappings() {
        JvmModuleProtoBuf.Module.Builder builder = JvmModuleProtoBuf.Module.newBuilder();
        String outputFilePath = getMappingFileName(state.getModuleName());

        StringTableImpl stringTable = new StringTableImpl();
        ClassFileUtilsKt.addDataFromCompiledModule(builder, packagePartRegistry, stringTable, state);

        Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable> tables = stringTable.buildProto();
        builder.setStringTable(tables.getFirst());
        builder.setQualifiedNameTable(tables.getSecond());

        JvmModuleProtoBuf.Module moduleProto = builder.build();

        generators.put(outputFilePath, new OutAndSourceFileList(CollectionsKt.toList(sourceFiles)) {
            @Override
            public byte[] asBytes(ClassBuilderFactory factory) {
                int flags = 0;
                if (state.getLanguageVersionSettings().getFlag(JvmAnalysisFlags.getStrictMetadataVersionSemantics())) {
                    flags |= ModuleMapping.STRICT_METADATA_VERSION_SEMANTICS_FLAG;
                }
                return ModuleMappingKt.serializeToByteArray(moduleProto, state.getMetadataVersion(), flags);
            }

            @Override
            public String asText(ClassBuilderFactory factory) {
                return new String(asBytes(factory), StandardCharsets.UTF_8);
            }
        });
    }

    @NotNull
    @Override
    public List<OutputFile> asList() {
        done();
        return getCurrentOutput();
    }

    @NotNull
    public List<OutputFile> getCurrentOutput() {
        return CollectionsKt.map(generators.keySet(), OutputClassFile::new);
    }

    @Override
    @Nullable
    public OutputFile get(@NotNull String relativePath) {
        return generators.containsKey(relativePath) ? new OutputClassFile(relativePath) : null;
    }

    @NotNull
    @TestOnly
    public String createText() {
        return createText(null);
    }

    private static class ModuleMappingException extends RuntimeException {
        public ModuleMappingException(String message) {
            super(message);
        }
    }

    @NotNull
    @TestOnly
    public String createText(@Nullable String ignorePrefixPath) {
        // NB this method is frequently used in JVM BE tests to display generated bytecode in case of test failure.
        // It should be permissive, and should try to handle exceptions gracefully (otherwise you would make JVM BE devs unhappy).

        StringBuilder answer = new StringBuilder();

        for (OutputFile file : asList()) {
            if (ignorePrefixPath != null && file.getRelativePath().startsWith(ignorePrefixPath)) continue;
            File relativePath = new File(file.getRelativePath());
            answer.append("@").append(relativePath).append('\n');
            switch (FilesKt.getExtension(relativePath)) {
                case "class":
                    answer.append(file.asText());
                    break;
                case "kotlin_module": {
                    try {
                        ModuleMapping mapping = ModuleMappingUtilKt.loadModuleMapping(
                                ModuleMapping.Companion, file.asByteArray(), relativePath.getPath(),
                                CompilerDeserializationConfiguration.Default.INSTANCE,
                                version -> {
                                    throw new ModuleMappingException("Generated module has incompatible JVM metadata version: " + version);
                                }
                        );
                        for (Map.Entry<String, PackageParts> entry : mapping.getPackageFqName2Parts().entrySet()) {
                            FqName packageFqName = new FqName(entry.getKey());
                            PackageParts packageParts = entry.getValue();
                            answer.append("<package ").append(packageFqName).append(": ").append(packageParts.getParts()).append(">\n");
                        }
                        break;
                    } catch (ModuleMappingException e) {
                        answer.append(relativePath).append(": ").append(e.getMessage()).append("\n");
                        break;
                    }
                }
                default:
                    answer.append("Unknown output file: ").append(file);
            }
        }

        return answer.toString();
    }

    @NotNull
    @TestOnly
    public Map<String, String> createTextForEachFile() {
        Map<String, String> answer = new LinkedHashMap<>();
        for (OutputFile file : asList()) {
            answer.put(file.getRelativePath(), file.asText());
        }
        return answer;
    }

    @NotNull
    public PackageCodegen forPackage(@NotNull FqName fqName, @NotNull Collection<KtFile> files) {
        assert !isDone : "Already done!";
        sourceFiles.addAll(toIoFilesIgnoringNonPhysical(files));
        return new PackageCodegenImpl(state, files, fqName);
    }

    @NotNull
    public MultifileClassCodegen forMultifileClass(@NotNull FqName facadeFqName, @NotNull Collection<KtFile> files) {
        assert !isDone : "Already done!";
        sourceFiles.addAll(toIoFilesIgnoringNonPhysical(files));
        return new MultifileClassCodegenImpl(state, files, facadeFqName);
    }

    public void registerSourceFiles(@NotNull Collection<File> files) {
        for (File file : files) {
            // We ignore non-physical files here, because this code is needed to tell the make what inputs affect which outputs
            // a non-physical file cannot be processed by make
            if (file == null) continue;
            sourceFiles.add(file);
        }
    }

    @NotNull
    private static List<File> toIoFilesIgnoringNonPhysical(@NotNull Collection<? extends PsiFile> psiFiles) {
        List<File> result = new ArrayList<>(psiFiles.size());
        for (PsiFile psiFile : psiFiles) {
            if (psiFile == null) continue;
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
            try {
                return generators.get(relativeClassFilePath).asBytes(builderFactory);
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Error generating class file " + this.toString() + ": " + e.getMessage(), e);
            }
        }

        @NotNull
        @Override
        public String asText() {
            try {
                return generators.get(relativeClassFilePath).asText(builderFactory);
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Error generating class file " + this.toString() + ": " + e.getMessage(), e);
            }
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
            synchronized(this) {
                return factory.asBytes(classBuilder);
            }
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

    public void removeClasses(Set<String> classNamesToRemove) {
        for (String classInternalName : classNamesToRemove) {
            generators.remove(classInternalName + ".class");
        }
    }

    @TestOnly
    public List<KtFile> getInputFiles() {
        return state.getFiles();
    }
}
