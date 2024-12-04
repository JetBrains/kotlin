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

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.storage.NullableLazyValue;
import org.jetbrains.kotlin.types.KotlinType;

public abstract class VariableDescriptorWithInitializerImpl extends VariableDescriptorImpl {
    private final boolean isVar;

    protected NullableLazyValue<ConstantValue<?>> compileTimeInitializer;
    protected Function0<NullableLazyValue<ConstantValue<?>>> compileTimeInitializerFactory;

    public VariableDescriptorWithInitializerImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @Nullable KotlinType outType,
            boolean isVar,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, outType, source);

        this.isVar = isVar;
    }

    @Override
    public boolean isVar() {
        return isVar;
    }

    @Nullable
    @Override
    public ConstantValue<?> getCompileTimeInitializer() {
        if (compileTimeInitializer != null) {
            return compileTimeInitializer.invoke();
        }
        return null;
    }

    public void setCompileTimeInitializerFactory(@NotNull Function0<NullableLazyValue<ConstantValue<?>>> compileTimeInitializerFactory) {
        assert !isVar() : "Constant value for variable initializer should be recorded only for final variables: " + getName();
        setCompileTimeInitializer(null, compileTimeInitializerFactory);
    }

    public void setCompileTimeInitializer(
            @Nullable NullableLazyValue<ConstantValue<?>> compileTimeInitializer,
            @NotNull Function0<NullableLazyValue<ConstantValue<?>>> compileTimeInitializerFactory
    ) {
        assert !isVar() : "Constant value for variable initializer should be recorded only for final variables: " + getName();
        this.compileTimeInitializerFactory = compileTimeInitializerFactory;
        this.compileTimeInitializer = compileTimeInitializer != null ? compileTimeInitializer : compileTimeInitializerFactory.invoke();
    }

    @Override
    public void cleanCompileTimeInitializerCache() {
        this.compileTimeInitializer = compileTimeInitializerFactory.invoke();
    }
}
