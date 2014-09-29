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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Function;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.lang.descriptors.ClassDescriptorWithResolutionScopes;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.ExceptionTracker;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collection;
import java.util.Map;

public interface BodiesResolveContext extends GlobalContext {
    @NotNull
    @Override
    StorageManager getStorageManager();
    @NotNull
    @Override
    ExceptionTracker getExceptionTracker();

    @ReadOnly
    Collection<JetFile> getFiles();

    @Mutable
    Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses();
    @Mutable
    Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers();
    @Mutable
    Map<JetScript, ScriptDescriptor> getScripts();

    @Mutable
    Map<JetProperty, PropertyDescriptor> getProperties();
    @Mutable
    Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions();

    Function<JetDeclaration, JetScope> getDeclaringScopes();
    DataFlowInfo getOuterDataFlowInfo();

    @NotNull
    TopDownAnalysisParameters getTopDownAnalysisParameters();

    boolean completeAnalysisNeeded(@NotNull PsiElement element);
}
