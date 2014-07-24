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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamAdapterDescriptor;

public class SamCodegenUtil {
    @Nullable
    public static FunctionDescriptor getOriginalIfSamAdapter(@NotNull FunctionDescriptor fun) {
        FunctionDescriptor original = fun.getOriginal();
        if (original instanceof SamAdapterDescriptor<?>) {
            return ((SamAdapterDescriptor<?>) original).getOriginForSam();
        }

        if (original.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            for (FunctionDescriptor overridden : original.getOverriddenDescriptors()) {
                FunctionDescriptor originalIfSamAdapter = getOriginalIfSamAdapter(overridden);
                if (originalIfSamAdapter != null) {
                    return originalIfSamAdapter;
                }
            }
        }

        return null;
    }

    private SamCodegenUtil() {
    }
}
