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

package org.jetbrains.jet.plugin.codeInsight;

import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.OverrideResolver;

import java.util.HashSet;
import java.util.Set;

public class OverrideMethodsHandler extends OverrideImplementMethodsHandler {
    @Override
    protected Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor) {
        final Set<CallableMemberDescriptor> superMethods = OverrideResolver.collectSuperMethods(descriptor).keySet();
        for (CallableMemberDescriptor member : descriptor.getDeclaredCallableMembers()) {
            superMethods.removeAll(member.getOverriddenDescriptors());
        }
        Set<CallableMemberDescriptor> result = new HashSet<CallableMemberDescriptor>();
        for (CallableMemberDescriptor superMethod : superMethods) {
            if (superMethod.getModality().isOverridable()) {
                result.add(superMethod);
            }
        }
        return result;
    }

    @Override
    protected String getChooserTitle() {
        return "Override Members";
    }

    @Override
    protected String getNoMethodsFoundHint() {
        return "No methods to override have been found";
    }
}
