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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.callableDescriptorToDeclaration;

public class LibrarySourceHacks {
    private LibrarySourceHacks() {
    }

    public static final Key<Boolean> SKIP_TOP_LEVEL_MEMBERS = Key.create("SKIP_TOP_LEVEL_MEMBERS"); // used when analyzing library source


    public static <D extends CallableDescriptor> List<D> filterOutMembersFromLibrarySource(Collection<D> members, BindingTrace trace) {
        List<D> filteredMembers = Lists.newArrayList();
        for (D member : members) {
            if (!shouldSkip(member, trace)) {
                filteredMembers.add(member);
            }
        }
        return filteredMembers;
    }

    private static boolean shouldSkip(CallableDescriptor member, BindingTrace trace) {
        CallableDescriptor original = member.getOriginal();
        if (!(original instanceof CallableMemberDescriptor)) {
            return false;
        }
        if (!(original.getContainingDeclaration() instanceof PackageFragmentDescriptor)) {
            return false;
        }

        PsiElement declaration = callableDescriptorToDeclaration(trace.getBindingContext(),
                                                                 (CallableMemberDescriptor) original);
        if (declaration == null) {
            return false;
        }
        PsiFile file = declaration.getContainingFile();
        return file != null && Boolean.TRUE.equals(file.getUserData(SKIP_TOP_LEVEL_MEMBERS));
    }
}
