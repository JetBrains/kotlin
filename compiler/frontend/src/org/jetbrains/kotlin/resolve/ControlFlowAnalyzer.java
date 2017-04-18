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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Map;

import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class ControlFlowAnalyzer {
    @NotNull private final BindingTrace trace;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final LanguageVersionSettings languageVersionSettings;

    public ControlFlowAnalyzer(
            @NotNull BindingTrace trace, @NotNull KotlinBuiltIns builtIns, @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.trace = trace;
        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
    }

    public void process(@NotNull BodiesResolveContext c) {
        for (KtFile file : c.getFiles()) {
            checkDeclarationContainer(c, file);
        }
        for (KtClassOrObject aClass : c.getDeclaredClasses().keySet()) {
            checkDeclarationContainer(c, aClass);
        }
        for (KtScript script : c.getScripts().keySet()) {
            checkDeclarationContainer(c, script);
        }
        for (KtSecondaryConstructor constructor : c.getSecondaryConstructors().keySet()) {
            checkSecondaryConstructor(constructor);
        }
        for (Map.Entry<KtNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            KtNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();
            KotlinType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType()
                                               ? NO_EXPECTED_TYPE
                                               : functionDescriptor.getReturnType();
            checkFunction(c, function, expectedReturnType);
        }
        for (Map.Entry<KtProperty, PropertyDescriptor> entry : c.getProperties().entrySet()) {
            KtProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();
            checkProperty(c, property, propertyDescriptor);
        }
    }

    private void checkSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
        ControlFlowInformationProvider controlFlowInformationProvider =
                new ControlFlowInformationProvider(constructor, trace, languageVersionSettings);
        controlFlowInformationProvider.checkDeclaration();
        controlFlowInformationProvider.checkFunction(builtIns.getUnitType());
    }

    private void checkDeclarationContainer(@NotNull BodiesResolveContext c, KtDeclarationContainer declarationContainer) {
        // A pseudocode of class/object initialization corresponds to a class/object
        // or initialization of properties corresponds to a package declared in a file
        ControlFlowInformationProvider controlFlowInformationProvider =
                new ControlFlowInformationProvider((KtElement) declarationContainer, trace, languageVersionSettings);
        if (c.getTopDownAnalysisMode().isLocalDeclarations()) {
            controlFlowInformationProvider.checkForLocalClassOrObjectMode();
            return;
        }
        controlFlowInformationProvider.checkDeclaration();
    }

    private void checkProperty(@NotNull BodiesResolveContext c, KtProperty property, PropertyDescriptor propertyDescriptor) {
        for (KtPropertyAccessor accessor : property.getAccessors()) {
            PropertyAccessorDescriptor accessorDescriptor = accessor.isGetter()
                                                            ? propertyDescriptor.getGetter()
                                                            : propertyDescriptor.getSetter();
            assert accessorDescriptor != null : "no property accessor descriptor " + accessor.getText();
            KotlinType returnType = accessorDescriptor.getReturnType();
            checkFunction(c, accessor, returnType);
        }
    }

    private void checkFunction(@NotNull BodiesResolveContext c, @NotNull KtDeclarationWithBody function, @Nullable KotlinType expectedReturnType) {
        ControlFlowInformationProvider controlFlowInformationProvider =
                new ControlFlowInformationProvider(function, trace, languageVersionSettings);
        if (c.getTopDownAnalysisMode().isLocalDeclarations()) {
            controlFlowInformationProvider.checkForLocalClassOrObjectMode();
            return;
        }
        controlFlowInformationProvider.checkDeclaration();
        controlFlowInformationProvider.checkFunction(expectedReturnType);
    }
}
