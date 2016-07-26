/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.builder;

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.ClassBuilder;
import org.jetbrains.kotlin.codegen.ClassBuilderFactory;
import org.jetbrains.kotlin.codegen.ClassBuilderMode;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;

/*package*/ class KotlinLightClassBuilderFactory implements ClassBuilderFactory {
    private final Stack<StubElement> stubStack;

    public KotlinLightClassBuilderFactory(Stack<StubElement> stubStack) {
        this.stubStack = stubStack;
    }

    @NotNull
    @Override
    public ClassBuilderMode getClassBuilderMode() {
        return ClassBuilderMode.LIGHT_CLASSES;
    }

    @NotNull
    @Override
    public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
        return new StubClassBuilder(stubStack);
    }

    @Override
    public String asText(ClassBuilder builder) {
        throw new UnsupportedOperationException("asText is not implemented"); // TODO
    }

    @Override
    public byte[] asBytes(ClassBuilder builder) {
        throw new UnsupportedOperationException("asBytes is not implemented"); // TODO
    }

    @Override
    public void close() {

    }
}
