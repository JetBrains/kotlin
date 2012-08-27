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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetTypeParameterListOwner;

import javax.inject.Inject;

/**
 * @author Stepan Koltsov
 */
public class MemberCodegen {

    @NotNull
    private GenerationState state;

    @Inject
    public void setState(@NotNull GenerationState state) {
        this.state = state;
    }


    public void generateFunctionOrProperty(
            @NotNull JetTypeParameterListOwner functionOrProperty,
            @NotNull CodegenContext context, @NotNull ClassBuilder classBuilder
    ) {
        FunctionCodegen functionCodegen = new FunctionCodegen(context, classBuilder, state);
        if (functionOrProperty instanceof JetNamedFunction) {
            try {
                functionCodegen.gen((JetNamedFunction) functionOrProperty);
            }
            catch (CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate function " + functionOrProperty.getName(), e, functionOrProperty);
            }
        }
        else if (functionOrProperty instanceof JetProperty) {
            try {
                new PropertyCodegen(context, classBuilder, functionCodegen, state).gen((JetProperty) functionOrProperty);
            }
            catch (CompilationException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CompilationException("Failed to generate property " + functionOrProperty.getName(), e, functionOrProperty);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown parameter: " + functionOrProperty);
        }
    }
}
