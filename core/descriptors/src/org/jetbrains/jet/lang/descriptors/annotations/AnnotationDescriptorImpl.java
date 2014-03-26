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

package org.jetbrains.jet.lang.descriptors.annotations;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collections;
import java.util.Map;

public class AnnotationDescriptorImpl implements AnnotationDescriptor {
    private JetType annotationType;
    private final Map<ValueParameterDescriptor, CompileTimeConstant<?>> valueArguments = Maps.newHashMap();
    private boolean valueArgumentsResolved = false;

    @Override
    @NotNull
    public JetType getType() {
        return annotationType;
    }

    @Override
    @Nullable
    public CompileTimeConstant<?> getValueArgument(@NotNull ValueParameterDescriptor valueParameterDescriptor) {
        return getAllValueArguments().get(valueParameterDescriptor);
    }

    @Override
    @NotNull
    public Map<ValueParameterDescriptor, CompileTimeConstant<?>> getAllValueArguments() {
        // TODO: this assertion does not hold now, but this whole class will be gone before long, so I'm not fixing it
        //assert valueArgumentsResolved : "Value arguments are not resolved yet for [" + getType() + "]";
        return Collections.unmodifiableMap(valueArguments);
    }

    public void setAnnotationType(@NotNull JetType annotationType) {
        this.annotationType = annotationType;
    }

    public void setValueArgument(@NotNull ValueParameterDescriptor name, @NotNull CompileTimeConstant<?> value) {
        assert !valueArgumentsResolved : "Value arguments are already resolved for " + this;
        valueArguments.put(name, value);
    }

    public void markValueArgumentsResolved() {
        this.valueArgumentsResolved = true;
    }

    public boolean areValueArgumentsResolved() {
        return valueArgumentsResolved;
    }

    @Override
    public String toString() {
        return DescriptorRenderer.TEXT.renderAnnotation(this);
    }
}
