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

package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface OverloadResolutionResults<D extends CallableDescriptor> {
    enum Code {
        SUCCESS(true),
        NAME_NOT_FOUND(false),
        SINGLE_CANDIDATE_ARGUMENT_MISMATCH(false),
        AMBIGUITY(false),
        MANY_FAILED_CANDIDATES(false),
        INCOMPLETE_TYPE_INFERENCE(false);

        private final boolean success;

        Code(boolean success) {
            this.success = success;
        }

        boolean isSuccess() {
            return success;
        }
    }

    @NotNull
    Collection<? extends ResolvedCall<? extends D>> getResultingCalls();

    @NotNull
    ResolvedCall<D> getResultingCall();

    @NotNull
    D getResultingDescriptor();

    @NotNull
    Code getResultCode();

    boolean isSuccess();

    boolean isSingleResult();

    boolean isNothing();

    boolean isAmbiguity();
}
