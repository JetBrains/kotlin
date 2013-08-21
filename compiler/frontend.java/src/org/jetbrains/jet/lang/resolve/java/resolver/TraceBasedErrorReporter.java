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

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.util.slicedmap.BasicWritableSlice;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;

public class TraceBasedErrorReporter implements ErrorReporter {
    public static final WritableSlice<AbiVersionErrorLocation, Integer> ABI_VERSION_ERRORS =
            new BasicWritableSlice<AbiVersionErrorLocation, Integer>(Slices.ONLY_REWRITE_TO_EQUAL, true);
    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Override
    public void reportIncompatibleAbiVersion(@NotNull FqName fqName, @NotNull VirtualFile file, int actualVersion) {
        trace.record(ABI_VERSION_ERRORS, new AbiVersionErrorLocation(fqName, file), actualVersion);
    }

    public static final class AbiVersionErrorLocation {
        @NotNull
        private final FqName classFqName;
        @NotNull
        private final VirtualFile file;

        public AbiVersionErrorLocation(@NotNull FqName name, @NotNull VirtualFile file) {
            this.classFqName = name;
            this.file = file;
        }

        @NotNull
        public FqName getClassFqName() {
            return classFqName;
        }

        @NotNull
        public String getPath() {
            return file.getPath();
        }
    }
}
