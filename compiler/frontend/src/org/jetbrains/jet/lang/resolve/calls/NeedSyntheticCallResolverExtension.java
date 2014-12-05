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

package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import static org.jetbrains.jet.lang.resolve.BindingContext.NEED_SYNTHETIC_ACCESSOR;

public class NeedSyntheticCallResolverExtension implements CallResolverExtension {

    @Override
    public  <F extends CallableDescriptor> void run(
            @NotNull ResolvedCall<F> resolvedCall,
            @NotNull BasicCallResolutionContext context
    ) {
        CallableDescriptor targetDescriptor = resolvedCall.getResultingDescriptor();
        if (needSyntheticAccessor(context.scope, targetDescriptor)) {
            context.trace.record(NEED_SYNTHETIC_ACCESSOR, (CallableMemberDescriptor) targetDescriptor.getOriginal(), Boolean.TRUE);
        }
    }

    //Necessary synthetic accessors in outer classes generated via old logic: CodegenContext.getAccessor
    //Generation of accessors in nested classes (to invoke from outer,
    //      e.g.: from class to classobject) controlled via NEED_SYNTHETIC_ACCESSOR slice
    private boolean needSyntheticAccessor(JetScope invokationScope, CallableDescriptor targetDescriptor) {
        return targetDescriptor instanceof CallableMemberDescriptor &&
               Visibilities.isPrivate(targetDescriptor.getVisibility()) &&
               targetDescriptor.getContainingDeclaration() != invokationScope.getContainingDeclaration().getContainingDeclaration();
    }
}
