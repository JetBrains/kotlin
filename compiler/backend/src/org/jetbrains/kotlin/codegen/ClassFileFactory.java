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
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.AnalysisFlag;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DescriptorUtilKt;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.kotlin.ModuleMappingUtilKt;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMappingKt;
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.serialization.StringTableImpl;
import org.jetbrains.org.objectweb.asm.Type;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.getMappingFileName;

public class ClassFileFactory implements OutputFileCollection {
    private final GenerationState state;
    private final ClassBuilderFactory builderFactory;
    private final Map<String, OutAndSourceFileList> generators = new LinkedHashMap<>();

    private boolean isDone = false;

    private final Set<File> sourceFiles = new HashSet<>();
    private final Map<String, PackageParts> partsGroupedByPackage = new LinkedHashMap<>();

    public ClassFileFactory(@NotNull GenerationState state, @NotNull ClassBuilderFactory builderFactory) {
        this.state = state;
        this.builderFactory = builderFactory;
    }

    public GenerationState getGenerationState() {
        return state;
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

    public void done() {
        if (!isDone) {
            isDone = true;
            writeModuleMappings();
        }
    }

    public void releaseGeneratedOutput() {
        generators.clear();
    }

    private void writeModuleMappings() {
        JvmModuleProtoBuf.Module.Builder builder = JvmModuleProtoBuf.Module.newBuilder();
        String outputFilePath = getMappingFileName(state.getModuleName());

        for (PackageParts part : ClassFileUtilsKt.addCompiledPartsAndSort(partsGroupedByPackage.values(), state)) {
            part.addTo(builder);
        }

        List<String> experimental = state.getLanguageVersionSettings().getFlag(AnalysisFlag.getExperimental());
        if (!experimental.isEmpty()) {
            writeExperimentalMarkers(state.getModule(), builder, experimental);
        }

        JvmModuleProtoBuf.Module moduleProto = builder.build();

        generators.put(outputFilePath, new OutAndSourceFileList(CollectionsKt.toList(sourceFiles)) {
            @Override
            public byte[] asBytes(ClassBuilderFactory factory) {
                return ModuleMappingKt.serializeToByteArray(moduleProto, state.getMetadataVersion().toArray());
            }

            @Override
            public String asText(ClassBuilderFactory factory) {
                try {
                    return new String(asBytes(factory), "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void writeExperimentalMarkers(
            @NotNull ModuleDescriptor module,
            @NotNull JvmModuleProtoBuf.Module.Builder builder,
            @NotNull List<String> experimental
    ) {
        StringTableImpl stringTable = new StringTableImpl();
        for (String fqName : experimental) {
            ClassDescriptor descriptor =
                    DescriptorUtilKt.resolveClassByFqName(module, new FqName(fqName), NoLookupLocation.FOR_ALREADY_TRACKED);
            if (descriptor != null) {
                ProtoBuf.Annotation.Builder annotation = ProtoBuf.Annotation.newBuilder();
                ClassId classId = DescriptorUtilsKt.getClassId(descriptor);
                if (classId != null) {
                    annotation.setId(stringTable.getQualifiedClassNameIndex(classId.asString(), false));
                    builder.addAnnotation(annotation);
                }
            }
        }
        Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable> tables = stringTable.buildProto();
        builder.setStringTable(tables.getFirst());
        builder.setQualifiedNameTable(tables.getSecond());
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

    @NotNull
    @TestOnly
    public String createText(@Nullable String ignorePrefixPath) {
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
                    ModuleMapping mapping = ModuleMappingUtilKt.loadModuleMapping(
                            ModuleMapping.Companion, file.asByteArray(), relativePath.getPath(),
                            CompilerDeserializationConfiguration.Default.INSTANCE, version -> {
                                throw new IllegalStateException("Version of the generated module cannot be incompatible: " + version);
                            }
                    );
                    for (Map.Entry<String, PackageParts> entry : mapping.getPackageFqName2Parts().entrySet()) {
                        FqName packageFqName = new FqName(entry.getKey());
                        PackageParts packageParts = entry.getValue();
                        answer.append("<package ").append(packageFqName).append(": ").append(packageParts.getParts()).append(">\n");
                    }
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unknown OutputFile: " + file);
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
        registerSourceFiles(files);
        return state.getCodegenFactory().createPackageCodegen(state, files, fqName, buildNewPackagePartRegistry(fqName));
    }

    @NotNull
    public MultifileClassCodegen forMultifileClass(@NotNull FqName facadeFqName, @NotNull Collection<KtFile> files) {
        assert !isDone : "Already done!";
        registerSourceFiles(files);
        return state.getCodegenFactory().createMultifileClassCodegen(state, files, facadeFqName, buildNewPackagePartRegistry(facadeFqName.parent()));
    }

    private PackagePartRegistry buildNewPackagePartRegistry(@NotNull FqName packageFqName) {
        String packageFqNameAsString = packageFqName.asString();
        return (partInternalName, facadeInternalName) -> {
            PackageParts packageParts = partsGroupedByPackage.computeIfAbsent(packageFqNameAsString, PackageParts::new);
            packageParts.addPart(partInternalName, facadeInternalName);
        };
    }

    private void registerSourceFiles(Collection<KtFile> files) {
        sourceFiles.addAll(toIoFilesIgnoringNonPhysical(files));
    }

    @NotNull
    private static List<File> toIoFilesIgnoringNonPhysical(@NotNull Collection<? extends PsiFile> psiFiles) {
        List<File> result = new ArrayList<>(psiFiles.size());
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
