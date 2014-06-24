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

package org.jetbrains.jet.codegen.optimization.boxing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.List;

public class RedundantBoxingMethodTransformer extends MethodTransformer {
    public RedundantBoxingMethodTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(String owner, @NotNull MethodNode node) {
        for (BoxedBasicValue value : analyze(owner, node)) {
            for (AbstractInsnNode insnNode : value.getAssociatedInsns()) {
                node.instructions.remove(insnNode);
            }
        }

        super.transform(owner, node);
    }

    private static List<BoxedBasicValue> analyze(@NotNull String owner, @NotNull MethodNode node) {
        RedundantBoxingInterpreter interpreter = new RedundantBoxingInterpreter(node.instructions);
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(interpreter);

        try {
            analyzer.analyze(owner, node);
        }
        catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }

        return interpreter.getCandidatesBoxedValues();
    }
}
