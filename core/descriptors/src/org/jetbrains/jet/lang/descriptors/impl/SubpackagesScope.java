/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;
import org.jetbrains.jet.utils.Printer;
import org.jetbrains.jet.utils.UtilsPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SubpackagesScope extends JetScopeImpl {
    private final PackageViewDescriptor packageView;

    public SubpackagesScope(PackageViewDescriptor packageView) {
        this.packageView = packageView;

    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return packageView;
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return name.isSpecial() ? null : packageView.getModule().getPackage(packageView.getFqName().child(name));
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        Collection<FqName> subFqNames = packageView.getModule().getPackageFragmentProvider().getSubPackagesOf(packageView.getFqName());
        List<DeclarationDescriptor> result = new ArrayList<DeclarationDescriptor>(subFqNames.size());
        for (FqName subFqName : subFqNames) {
            UtilsPackage.addIfNotNull(result, getPackage(subFqName.shortName()));
        }
        return result;
    }

    @Override
    public void printScopeStructure(@NotNull Printer p) {
        p.println(getClass().getSimpleName(), " {");
        p.pushIndent();

        p.println("thisDescriptor = ", packageView);

        p.popIndent();
        p.println("}");
    }
}
