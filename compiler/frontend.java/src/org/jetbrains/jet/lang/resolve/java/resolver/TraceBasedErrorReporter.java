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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClass;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;

import static org.jetbrains.jet.lang.diagnostics.Errors.CANNOT_INFER_VISIBILITY;

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
        PsiElement element = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), descriptor);
        if (element instanceof JetDeclaration) {
            trace.report(CANNOT_INFER_VISIBILITY.on((JetDeclaration) element));
        }
    }

    @Override
    public void reportLoadingError(@NotNull String message, @Nullable Exception exception) {
        LOG.error(message, exception);
    }
}
