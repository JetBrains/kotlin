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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.impl.SubpackagesScope;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

public class JetModuleUtil {
    public static JetScope getSubpackagesOfRootScope(@NotNull ModuleDescriptor module) {
        return getRootPackageScope(module, /* scopeIncludingMembers = */ false);
    }

    public static JetScope getImportsResolutionScope(ModuleDescriptor module, boolean includeRootPackageClasses) {
        return getRootPackageScope(module, /* scopeIncludingMembers = */ includeRootPackageClasses);
    }

    private static JetScope getRootPackageScope(ModuleDescriptor module, boolean scopeIncludingMembers) {
        PackageViewDescriptor rootPackage = module.getPackage(FqName.ROOT);
        assert rootPackage != null : "Couldn't find root package for " + module;

        return scopeIncludingMembers ? rootPackage.getMemberScope() : new SubpackagesScope(rootPackage);
    }
}
