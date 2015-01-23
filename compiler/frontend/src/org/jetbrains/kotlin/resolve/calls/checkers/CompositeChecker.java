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

package org.jetbrains.kotlin.resolve.calls.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

import java.util.List;

public class CompositeChecker implements CallChecker {

    private final List<CallChecker> extensions;

    public CompositeChecker(@NotNull List<CallChecker> extensions) {
        this.extensions = extensions;
    }

    @Override
    public <F extends CallableDescriptor> void check(
            @NotNull ResolvedCall<F> resolvedCall,
            @NotNull BasicCallResolutionContext context
    ) {
        for (CallChecker resolverExtension : extensions) {
            resolverExtension.check(resolvedCall, context);
        }
    }
}
