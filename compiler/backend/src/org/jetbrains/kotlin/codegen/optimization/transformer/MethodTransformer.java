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

package org.jetbrains.kotlin.codegen.optimization.transformer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.*;

public abstract class MethodTransformer {
    @NotNull
    private static <V extends Value> Frame<V>[] runAnalyzer(
            @NotNull Analyzer<V> analyzer,
            @NotNull String internalClassName,
            @NotNull MethodNode node
    ) {
        try {
            return analyzer.analyze(internalClassName, node);
        }
        catch (AnalyzerException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    public static <V extends Value> Frame<V>[] analyze(
            @NotNull String internalClassName,
            @NotNull MethodNode node,
            @NotNull Interpreter<V> interpreter
    ) {
        try {
            FastMethodAnalyzer<V> analyser = new FastMethodAnalyzer<>(internalClassName, node, interpreter);
            return analyser.analyze();
        } catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    public abstract void transform(@NotNull String internalClassName, @NotNull MethodNode methodNode);
}
