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

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.scopes.*;

public class MutablePackageFragmentDescriptor extends PackageFragmentDescriptorImpl {

    public MutablePackageFragmentDescriptor(@NotNull ModuleDescriptor module, @NotNull FqName fqName) {
        super(module, fqName);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope() {
        return MemberScope.Empty.INSTANCE;
    }
}
