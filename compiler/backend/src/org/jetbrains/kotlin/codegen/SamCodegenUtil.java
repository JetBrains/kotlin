/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor;
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor;
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor;

public class SamCodegenUtil {
    @Nullable
    public static FunctionDescriptor getOriginalIfSamAdapter(@NotNull FunctionDescriptor fun) {
        if (fun instanceof SamAdapterDescriptor<?> || fun instanceof SamAdapterExtensionFunctionDescriptor) {
            //noinspection unchecked
            return ((SyntheticMemberDescriptor<FunctionDescriptor>) fun).getBaseDescriptorForSynthetic();
        }

        return null;
    }

    public static <T extends FunctionDescriptor> T resolveSamAdapter(@NotNull T descriptor) {
        FunctionDescriptor original = getOriginalIfSamAdapter(descriptor);
        return original != null ? (T) original : descriptor;
    }

    private SamCodegenUtil() {
    }
}
