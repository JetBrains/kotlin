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
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import javax.inject.Inject;
import java.util.List;

public class TraceBasedErrorReporter implements ErrorReporter {
    private static final Logger LOG = Logger.getInstance(TraceBasedErrorReporter.class);

    public static class AbiVersionErrorData {
        public final int actualVersion;
        public final ClassId classId;

        public AbiVersionErrorData(int actualVersion, @NotNull ClassId classId) {
            this.actualVersion = actualVersion;
            this.classId = classId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AbiVersionErrorData data = (AbiVersionErrorData) o;

            if (actualVersion != data.actualVersion) return false;
            if (!classId.equals(data.classId)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = actualVersion;
            result = 31 * result + classId.hashCode();
            return result;
        }
    }

    public static final WritableSlice<String, AbiVersionErrorData> ABI_VERSION_ERRORS = Slices.createCollectiveSlice();
    public static final WritableSlice<ClassDescriptor, List<String>> INCOMPLETE_HIERARCHY = Slices.createCollectiveSlice();

    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Override
    public void reportIncompatibleAbiVersion(@NotNull ClassId classId, @NotNull String filePath, int actualVersion) {
        trace.record(ABI_VERSION_ERRORS, filePath, new AbiVersionErrorData(actualVersion, classId));
    }

    @Override
    public void reportIncompleteHierarchy(@NotNull ClassDescriptor descriptor, @NotNull List<String> unresolvedSuperClasses) {
        trace.record(INCOMPLETE_HIERARCHY, descriptor, unresolvedSuperClasses);
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
