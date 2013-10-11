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

package org.jetbrains.jet.lang.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;

public interface KotlinCodeAnalyzer {
    ModuleDescriptor getModuleDescriptor();

    @NotNull
    ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject);

    @NotNull
    BindingContext getBindingContext();

    @NotNull
    DeclarationDescriptor resolveToDescriptor(JetDeclaration declaration);

    /**
     * Forces all descriptors to be resolved.
     *
     * Use this method when laziness plays against you, e.g. when lazy descriptors may be accessed in a multi-threaded setting
     */
    void forceResolveAll();
}
