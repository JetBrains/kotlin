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

package org.jetbrains.jet.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

public interface RedeclarationHandler {
    RedeclarationHandler DO_NOTHING = new RedeclarationHandler() {
        @Override
        public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        }
    };
    RedeclarationHandler THROW_EXCEPTION = new RedeclarationHandler() {
        @Override
        public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
            throw new IllegalStateException(
                    String.format("Redeclaration: %s (%s) and %s (%s) (no line info available)", DescriptorUtils.getFqName(first), first,
                                  DescriptorUtils.getFqName(second), second)
            );
        }
    };

    void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second);
}
