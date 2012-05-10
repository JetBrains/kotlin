/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;

import javax.rmi.CORBA.ClassDesc;
import java.util.List;

/**
 * @author abreslav
 */
public class AnnotationDescriptor {
    private JetType annotationType;
    private List<CompileTimeConstant<?>> valueArguments;

    @NotNull
    public JetType getType() {
        return annotationType;
    }

    @NotNull
    public List<CompileTimeConstant<?>> getValueArguments() {
        return valueArguments;
    }

    public void setAnnotationType(@NotNull JetType annotationType) {
        if (false && !(annotationType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            throw new IllegalStateException();
        }
        this.annotationType = annotationType;
    }

    public void setValueArguments(@NotNull List<CompileTimeConstant<?>> valueArguments) {
        this.valueArguments = valueArguments;
    }
}
