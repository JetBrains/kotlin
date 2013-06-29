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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.util.slicedmap.BasicWritableSlice;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.List;

/**
 * @see BindingContext
 */
public class JavaBindingContext {

    /**
     * @see BindingContext#NAMESPACE_IS_SRC
     */
    public static final WritableSlice<NamespaceDescriptor, JavaNamespaceKind> JAVA_NAMESPACE_KIND =
            Slices.createSimpleSlice();

    public static final WritableSlice<DeclarationDescriptor, List<String>> LOAD_FROM_JAVA_SIGNATURE_ERRORS =
            new BasicWritableSlice<DeclarationDescriptor, List<String>>(Slices.ONLY_REWRITE_TO_EQUAL, true);

    public static final WritableSlice<CallableDescriptor, Boolean> IS_DECLARED_IN_JAVA = Slices.createSimpleSlice();

    public static final WritableSlice<SimpleFunctionDescriptor, ClassDescriptorFromJvmBytecode> SAM_CONSTRUCTOR_TO_INTERFACE = Slices.createSimpleSlice();
    public static final WritableSlice<FunctionDescriptor, FunctionDescriptor> SAM_ADAPTER_FUNCTION_TO_ORIGINAL = Slices.createSimpleSlice();

    private JavaBindingContext() {
    }
}
