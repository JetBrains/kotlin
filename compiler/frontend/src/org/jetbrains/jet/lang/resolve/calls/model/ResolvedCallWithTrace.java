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

package org.jetbrains.jet.lang.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;

public interface ResolvedCallWithTrace<D extends CallableDescriptor> extends ResolvedCall<D>  {

    @NotNull
    ResolutionStatus getStatus();

    /**
     * Resolved call can have incomplete type parameters
     * if ResolutionStatus is INCOMPLETE_TYPE_INFERENCE (might be completed successfully)
     * or OTHER_ERROR (cannot be completed successfully, but if there's only one candidate, should be completed anyway).
     * @return true if resolved call has unknown type parameters (inference is incomplete)
     */
    boolean hasIncompleteTypeParameters();

    boolean isDirty();

    DelegatingBindingTrace getTrace();
}
