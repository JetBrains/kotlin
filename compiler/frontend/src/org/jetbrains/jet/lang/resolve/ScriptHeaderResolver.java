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

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ScriptDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.source.SourcePackage.toSourceElement;

// SCRIPT: Resolve declarations in scripts
public class ScriptHeaderResolver {

    public static final Key<Integer> PRIORITY_KEY = Key.create(JetScript.class.getName() + ".priority");

    @NotNull
    private MutablePackageFragmentProvider packageFragmentProvider;
    @NotNull
    private BindingTrace trace;

    @Inject
    public void setPackageFragmentProvider(@NotNull MutablePackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }



    public void processScriptHierarchy(@NotNull TopDownAnalysisContext c, @NotNull JetScript script, @NotNull WritableScope outerScope) {
        JetFile file = script.getContainingJetFile();
        FqName fqName = file.getPackageFqName();
        PackageFragmentDescriptor ns = packageFragmentProvider.getOrCreateFragment(fqName);

        Integer priority = getScriptPriority(script);

        FqName nameForScript = ScriptNameUtil.classNameForScript(script);
        Name className = nameForScript.shortName();
        ScriptDescriptorImpl scriptDescriptor = new ScriptDescriptorImpl(ns, priority, outerScope, className, toSourceElement(script));

        WritableScopeImpl scriptScope = new WritableScopeImpl(outerScope, scriptDescriptor, RedeclarationHandler.DO_NOTHING, "script");
        scriptScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        scriptDescriptor.setScopeForBodyResolution(scriptScope);

        c.getScripts().put(script, scriptDescriptor);

        trace.record(BindingContext.SCRIPT, script, scriptDescriptor);

        outerScope.addClassifierDescriptor(scriptDescriptor.getClassDescriptor());
    }

    public static int getScriptPriority(@NotNull JetScript script) {
        Integer priority = script.getUserData(PRIORITY_KEY);
        return priority == null ? 0 : priority;
    }

    public static void resolveScriptDeclarations(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetScript, ScriptDescriptor> e : c.getScripts().entrySet()) {
            JetScript declaration = e.getKey();
            ScriptDescriptorImpl descriptor = (ScriptDescriptorImpl) e.getValue();

            List<ValueParameterDescriptor> valueParameters = ScriptParameterResolver.resolveScriptParameters(
                    declaration,
                    descriptor
            );

            descriptor.setValueParameters(valueParameters);

            WritableScope scope = descriptor.getScopeForBodyResolution();
            scope.setImplicitReceiver(descriptor.getThisAsReceiverParameter());
            for (ValueParameterDescriptor valueParameterDescriptor : valueParameters) {
                scope.addVariableDescriptor(valueParameterDescriptor);
            }
        }
    }

}
