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

import com.google.common.collect.Maps;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;

import java.util.Map;

public class SamWrapperClasses {
    private final GenerationState state;

    private final Map<Pair<JavaClassDescriptor, JetFile>, Type> samInterfaceToWrapperClass = Maps.newHashMap();

    public SamWrapperClasses(GenerationState state) {
        this.state = state;
    }

    @NotNull
    public Type getSamWrapperClass(@NotNull final JavaClassDescriptor samInterface, @NotNull final JetFile file) {
        return ContainerUtil.getOrCreate(samInterfaceToWrapperClass, Pair.create(samInterface, file),
                                         new Factory<Type>() {
                                             @Override
                                             public Type create() {
                                                 return new SamWrapperCodegen(state, samInterface, null).genWrapper(file);
                                             }
                                         });
    }
}
