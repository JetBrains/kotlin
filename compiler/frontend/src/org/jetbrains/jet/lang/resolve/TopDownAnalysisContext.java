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
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.storage.ExceptionTracker;
import org.jetbrains.jet.storage.StorageManager;

import java.io.PrintStream;
import java.util.*;

public class TopDownAnalysisContext implements BodiesResolveContext {

    private DataFlowInfo outerDataFlowInfo = DataFlowInfo.EMPTY;

    private final Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> classes = Maps.newLinkedHashMap();
    private final Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> anonymousInitializers = Maps.newLinkedHashMap();
    protected final Map<JetFile, MutablePackageFragmentDescriptor> packageFragments = Maps.newHashMap();
    protected final Set<JetFile> files = new LinkedHashSet<JetFile>();
    private List<MutableClassDescriptor> classesTopologicalOrder = null;

    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Map<JetParameter, PropertyDescriptor> primaryConstructorParameterProperties = Maps.newHashMap();
    private Map<JetDeclaration, CallableMemberDescriptor> members = null;

    // File scopes - package scope extended with imports
    protected final Map<JetFile, WritableScope> fileScopes = Maps.newHashMap();

    public final Map<JetDeclarationContainer, DeclarationDescriptor> forDeferredResolver = Maps.newHashMap();

    public final Map<JetDeclarationContainer, JetScope> normalScope = Maps.newHashMap();

    private final Map<JetScript, ScriptDescriptor> scripts = Maps.newLinkedHashMap();

    private final TopDownAnalysisParameters topDownAnalysisParameters;

    private StringBuilder debugOutput;

    public TopDownAnalysisContext(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Override
    @NotNull
    public TopDownAnalysisParameters getTopDownAnalysisParameters() {
        return topDownAnalysisParameters;
    }

    public void debug(Object message) {
        if (debugOutput != null) {
            debugOutput.append(message).append("\n");
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    /*package*/ void enableDebugOutput() {
        if (debugOutput == null) {
            debugOutput = new StringBuilder();
        }
    }
    
    /*package*/ void printDebugOutput(PrintStream out) {
        if (debugOutput != null) {
            out.print(debugOutput);
        }
    }

    @Override
    public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        boolean result = containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
        if (!result) {
            debug(containingFile);
        }
        return result;
    }

    @Override
    public Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses() {
        return classes;
    }

    @Override
    public Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers() {
        return anonymousInitializers;
    }

    public Map<JetFile, WritableScope> getFileScopes() {
        return fileScopes;
    }

    public Map<JetFile, MutablePackageFragmentDescriptor> getPackageFragments() {
        return packageFragments;
    }

    @NotNull
    @Override
    public StorageManager getStorageManager() {
        return topDownAnalysisParameters.getStorageManager();
    }

    @NotNull
    @Override
    public ExceptionTracker getExceptionTracker() {
        return topDownAnalysisParameters.getExceptionTracker();
    }

    @Override
    public Collection<JetFile> getFiles() {
        return files;
    }

    public void addFile(@NotNull JetFile file) {
        files.add(file);
    }

    @Override
    @NotNull
    public Map<JetScript, ScriptDescriptor> getScripts() {
        return scripts;
    }

    public Map<JetParameter, PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    @Override
    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public Function<JetDeclaration, JetScope> getDeclaringScopes() {
        return Functions.forMap(declaringScopes);
    }

    public void registerDeclaringScope(@NotNull JetDeclaration declaration, @NotNull JetScope scope) {
        declaringScopes.put(declaration, scope);
    }

    @Override
    public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    public Map<JetDeclaration, CallableMemberDescriptor> getMembers() {
        if (members == null) {
            members = Maps.newHashMap();
            members.putAll(functions);
            members.putAll(properties);
            members.putAll(primaryConstructorParameterProperties);
        }
        return members;
    }

    @NotNull
    public List<MutableClassDescriptor> getClassesTopologicalOrder() {
        return classesTopologicalOrder;
    }

    public void setClassesTopologicalOrder(@NotNull List<MutableClassDescriptor> classesTopologicalOrder) {
        this.classesTopologicalOrder = classesTopologicalOrder;
    }

    @Override
    @NotNull
    public DataFlowInfo getOuterDataFlowInfo() {
        return outerDataFlowInfo;
    }

    public void setOuterDataFlowInfo(@NotNull DataFlowInfo outerDataFlowInfo) {
        this.outerDataFlowInfo = outerDataFlowInfo;
    }

    @NotNull
    public Collection<ClassDescriptorWithResolutionScopes> getAllClasses() {
        // SCRIPT: all classes are declared classes + script classes
        Collection<ClassDescriptorWithResolutionScopes> scriptClasses = KotlinPackage.map(
                getScripts().values(),
                new Function1<ScriptDescriptor, ClassDescriptorWithResolutionScopes>() {
                    @Override
                    public ClassDescriptorWithResolutionScopes invoke(ScriptDescriptor scriptDescriptor) {
                        return (ClassDescriptorWithResolutionScopes) scriptDescriptor.getClassDescriptor();
                    }
                }
        );
        return KotlinPackage.plus(getDeclaredClasses().values(), scriptClasses);
    }
}
