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

package org.jetbrains.kotlin.load.java.components;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import javax.inject.Inject;

public class TraceBasedErrorReporter implements ErrorReporter {
    private static final Logger LOG = Logger.getInstance(TraceBasedErrorReporter.class);

    public static final WritableSlice<VirtualFileKotlinClass, Integer> ABI_VERSION_ERRORS = Slices.createCollectiveSlice();

    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Override
    public void reportIncompatibleAbiVersion(@NotNull KotlinJvmBinaryClass kotlinClass, int actualVersion) {
        trace.record(ABI_VERSION_ERRORS, (VirtualFileKotlinClass) kotlinClass, actualVersion);
    }

    @Override
    public void reportCannotInferVisibility(@NotNull CallableMemberDescriptor descriptor) {
        OverrideResolver.createCannotInferVisibilityReporter(trace).invoke(descriptor);
    }

    @Override
    public void reportLoadingError(@NotNull String message, @Nullable Exception exception) {
        LOG.error(message, exception);
    }
}
