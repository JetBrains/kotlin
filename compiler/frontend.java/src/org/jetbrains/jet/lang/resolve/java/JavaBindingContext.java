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

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
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
    public static final WritableSlice<PackageFragmentDescriptor, JavaNamespaceKind> JAVA_NAMESPACE_KIND = Slices.createSimpleSlice();

    public static final WritableSlice<DeclarationDescriptor, List<String>> LOAD_FROM_JAVA_SIGNATURE_ERRORS = Slices.createCollectiveSlice();

    private JavaBindingContext() {
    }
}
