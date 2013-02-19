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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collection;
import java.util.Map;

public interface BodiesResolveContext {
    Collection<JetFile> getFiles();
    Map<JetClass, MutableClassDescriptor> getClasses();
    Map<JetObjectDeclaration, MutableClassDescriptor> getObjects();
    Map<JetProperty, PropertyDescriptor> getProperties();
    Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions();
    Function<JetDeclaration, JetScope> getDeclaringScopes();
    Map<JetScript, ScriptDescriptor> getScripts();
    Map<JetScript, WritableScope> getScriptScopes();

    void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters);

    boolean completeAnalysisNeeded(@NotNull PsiElement element);
}
