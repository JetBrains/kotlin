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
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

public class SubPackagesScope extends JetScopeImpl {
        private final PackageViewDescriptor packageView;

        public SubPackagesScope(@NotNull PackageViewDescriptor packageView) {
            this.packageView = packageView;
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return packageView;
        }

        @Override
        public PackageViewDescriptor getPackage(@NotNull Name name) {
            return packageView.getMemberScope().getPackage(name);
        }

        @Override
        public String toString() {
            return "Subpackages of the root package of sub-module " + packageView.getViewContext();
        }
    }
