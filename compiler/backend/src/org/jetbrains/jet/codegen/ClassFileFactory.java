/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.util.*;

/**
 * @author max
 * @author alex.tkachman
 */
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

    ClassBuilder newVisitor(String filePath) {
        state.getProgress().log("Emitting: " + filePath);
        final ClassBuilder answer = builderFactory.newClassBuilder();
        generators.put(filePath, answer);
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

    public NamespaceCodegen forNamespace(final FqName fqName, Collection<JetFile> files) {
        assert !isDone : "Already done!";
        NamespaceCodegen codegen = ns2codegen.get(fqName);
        if (codegen == null) {
            ClassBuilderOnDemand onDemand = new ClassBuilderOnDemand() {
                @NotNull
                @Override
                protected ClassBuilder createClassBuilder() {
                    return newVisitor(NamespaceCodegen.getJVMClassNameForKotlinNs(fqName).getInternalName() + ".class");
                }
            };
            codegen = new NamespaceCodegen(onDemand, fqName, state, files);
            ns2codegen.put(fqName, codegen);
        }

        return codegen;
    }

    public ClassBuilder forClassImplementation(ClassDescriptor aClass) {
        Type type = state.getTypeMapper().mapType(aClass.getDefaultType(), JetTypeMapperMode.IMPL);
        if (CodegenUtil.isPrimitive(type)) {
            throw new IllegalStateException("Codegen for primitive type is not possible: " + aClass);
        }
        return newVisitor(type.getInternalName() + ".class");
    }

    public ClassBuilder forNamespacepart(String name) {
        return newVisitor(name + ".class");
    }

    public ClassBuilder forTraitImplementation(ClassDescriptor aClass, GenerationState state) {
        return newVisitor(
                state.getTypeMapper().mapType(aClass.getDefaultType(), JetTypeMapperMode.TRAIT_IMPL).getInternalName() + ".class");
    }
}
