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

import com.google.common.collect.Lists;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.isPrimitive;

public final class ClassFileFactory extends GenerationStateAware {
    @NotNull private ClassBuilderFactory builderFactory;

    private final Map<FqName, NamespaceCodegen> ns2codegen = new HashMap<FqName, NamespaceCodegen>();
    private final Map<String, ClassBuilder> generators = new LinkedHashMap<String, ClassBuilder>();
    private boolean isDone = false;

    public ClassFileFactory(@NotNull GenerationState state) {
        super(state);
    }


    @Inject
    public void setBuilderFactory(@NotNull ClassBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    ClassBuilder newVisitor(String internalClassName, PsiFile sourceFile) {
        return newVisitor(internalClassName + ".class", Collections.singletonList(sourceFile));
    }
    
    private ClassBuilder newVisitor(String outputFilePath, Collection<? extends PsiFile> sourceFiles) {
        state.getProgress().reportOutput(toIoFilesIgnoringNonPhysical(sourceFiles), new File(outputFilePath));
        ClassBuilder answer = builderFactory.newClassBuilder();
        generators.put(outputFilePath, answer);
        return answer;
    }

    private void done() {
        if (!isDone) {
            isDone = true;
            for (NamespaceCodegen codegen : ns2codegen.values()) {
                codegen.done();
            }
        }
    }

    public String asText(String file) {
        done();
        return builderFactory.asText(generators.get(file));
    }

    public byte[] asBytes(String file) {
        done();
        return builderFactory.asBytes(generators.get(file));
    }

    public List<String> files() {
        done();
        return new ArrayList<String>(generators.keySet());
    }

    public String createText() {
        StringBuilder answer = new StringBuilder();

        List<String> files = files();
        for (String file : files) {
            //            if (!file.startsWith("kotlin/")) {
            answer.append("@").append(file).append('\n');
            answer.append(asText(file));
            //            }
        }

        return answer.toString();
    }

    public NamespaceCodegen forNamespace(final FqName fqName, final Collection<JetFile> files) {
        assert !isDone : "Already done!";
        NamespaceCodegen codegen = ns2codegen.get(fqName);
        if (codegen == null) {
            ClassBuilderOnDemand onDemand = new ClassBuilderOnDemand() {
                @NotNull
                @Override
                protected ClassBuilder createClassBuilder() {
                    return newVisitor(
                            NamespaceCodegen.getJVMClassNameForKotlinNs(fqName).getInternalName() + ".class",
                            files
                    );
                }
            };
            codegen = new NamespaceCodegen(onDemand, fqName, state, files);
            ns2codegen.put(fqName, codegen);
        }

        return codegen;
    }

    public ClassBuilder forClassImplementation(ClassDescriptor aClass, PsiFile sourceFile) {
        Type type = state.getTypeMapper().mapType(aClass.getDefaultType(), JetTypeMapperMode.IMPL);
        if (isPrimitive(type)) {
            throw new IllegalStateException("Codegen for primitive type is not possible: " + aClass);
        }
        return newVisitor(type.getInternalName(), sourceFile);
    }

    public ClassBuilder forNamespacepart(String internalName, PsiFile sourceFile) {
        return newVisitor(internalName, sourceFile);
    }

    public ClassBuilder forTraitImplementation(ClassDescriptor aClass, GenerationState state, PsiFile sourceFile) {
        return newVisitor(
                state.getTypeMapper().mapType(aClass.getDefaultType(), JetTypeMapperMode.TRAIT_IMPL).getInternalName(),
                sourceFile);
    }

    private static Collection<File> toIoFilesIgnoringNonPhysical(Collection<? extends PsiFile> psiFiles) {
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

}
