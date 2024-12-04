/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.binding;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.List;

public class CodegenBinding {
    public static final WritableSlice<Type, List<VariableDescriptorWithAccessors>> DELEGATED_PROPERTIES_WITH_METADATA =
            Slices.createSimpleSlice();

    static {
        BasicWritableSlice.initSliceDebugNames(CodegenBinding.class);
    }

    private CodegenBinding() {
    }

    @Nullable
    public static List<LocalVariableDescriptor> getLocalDelegatedProperties(@NotNull BindingContext bindingContext, @NotNull Type owner) {
        List<VariableDescriptorWithAccessors> properties = bindingContext.get(DELEGATED_PROPERTIES_WITH_METADATA, owner);
        return properties == null ? null : CollectionsKt.filterIsInstance(properties, LocalVariableDescriptor.class);
    }
}
