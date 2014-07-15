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
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame;
import org.jetbrains.org.objectweb.asm.tree.analysis.Value;

public abstract class MethodTransformer {
    private final MethodTransformer methodTransformer;

    protected MethodTransformer(MethodTransformer methodTransformer) {
        this.methodTransformer = methodTransformer;
    }

    protected static <V extends Value> Frame<V>[] runAnalyzer(
            @NotNull Analyzer<V> analyzer,
            @NotNull String owner,
            @NotNull MethodNode node
    ) {
        try {
            return analyzer.analyze(owner, node);
        }
        catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }
    }

    public void transform(@NotNull String owner, @NotNull MethodNode methodNode) {
        if (methodTransformer != null) {
            methodTransformer.transform(owner, methodNode);
        }
    }
}
