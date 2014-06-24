/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.optimization.transformer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.optimization.OptimizationUtils;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

public class ClassMethodsTransformer {
    private final MethodTransformer methodTransformer;

    public ClassMethodsTransformer(MethodTransformer methodTransformer) {
        this.methodTransformer = methodTransformer;
    }

    public void transform(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            methodTransformer.transform(classNode.name, methodNode);
        }
    }

    @NotNull
    public byte[] transform(@NotNull byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode(OptimizationUtils.API);
        classReader.accept(classNode, 0);
        transform(classNode);

        ClassWriter classWriter = new BinaryClassWriter();
        classNode.accept(classWriter);

        return classWriter.toByteArray();
    }

    private static class BinaryClassWriter extends ClassWriter {
        public BinaryClassWriter() {
            super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        }

        @Override
        protected String getCommonSuperClass(@NotNull String type1, @NotNull String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            }
            catch (Throwable t) {
                // @todo we might need at some point do more sophisticated handling
                return "java/lang/Object";
            }
        }
    }
}
