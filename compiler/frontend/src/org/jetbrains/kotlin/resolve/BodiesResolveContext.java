/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import kotlin.annotations.jvm.Mutable;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;

import java.util.Collection;
import java.util.Map;

public interface BodiesResolveContext {
    @ReadOnly
    Collection<KtFile> getFiles();

    @Mutable
    Map<KtClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses();
    @Mutable
    Map<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers();
    @Mutable
    Map<KtSecondaryConstructor, ClassConstructorDescriptor> getSecondaryConstructors();
    @Mutable
    Map<KtScript, ClassDescriptorWithResolutionScopes> getScripts();

    @Mutable
    Map<KtProperty, PropertyDescriptor> getProperties();
    @Mutable
    Map<KtNamedFunction, SimpleFunctionDescriptor> getFunctions();
    @Mutable
    Map<KtTypeAlias, TypeAliasDescriptor> getTypeAliases();
    @Mutable
    Map<KtDestructuringDeclarationEntry, PropertyDescriptor> getDestructuringDeclarationEntries();

    @Nullable
    LexicalScope getDeclaringScope(@NotNull KtDeclaration declaration);

    @NotNull
    DataFlowInfo getOuterDataFlowInfo();

    @NotNull
    TopDownAnalysisMode getTopDownAnalysisMode();
}
