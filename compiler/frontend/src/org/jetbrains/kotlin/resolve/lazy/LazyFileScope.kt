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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.SubpackagesScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.NoSubpackagesInPackageScope
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.sure
import java.util.ArrayList

class LazyFileScope(
        scopeChain: List<JetScope>,
        private val aliasImportResolver: LazyImportResolver,
        private val allUnderImportResolver: LazyImportResolver,
        containingDeclaration: PackageFragmentDescriptor,
        debugName: String
) : ChainedScope(containingDeclaration, debugName, *scopeChain.toTypedArray()) {

    public fun forceResolveAllImports() {
        aliasImportResolver.forceResolveAllContents()
        allUnderImportResolver.forceResolveAllContents()
    }

    public fun forceResolveImport(importDirective: JetImportDirective) {
        if (importDirective.isAllUnder()) {
            allUnderImportResolver.forceResolveImportDirective(importDirective)
        }
        else {
            aliasImportResolver.forceResolveImportDirective(importDirective)
        }
    }
}
