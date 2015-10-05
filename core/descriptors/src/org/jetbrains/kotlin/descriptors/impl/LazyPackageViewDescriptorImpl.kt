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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.storage.get
import org.jetbrains.kotlin.types.TypeSubstitutor

public class LazyPackageViewDescriptorImpl(
        override val module: ModuleDescriptorImpl,
        override val fqName: FqName,
        storageManager: StorageManager
) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {

    override val fragments: List<PackageFragmentDescriptor> by storageManager.createLazyValue {
        module.packageFragmentProvider.getPackageFragments(fqName)
    }

    override val memberScope: JetScope = LazyScopeAdapter(storageManager.createLazyValue {
        if (fragments.isEmpty()) {
            JetScope.Empty
        }
        else {
            val scopes = fragments.map { it.getMemberScope() } + SubpackagesScope(module, fqName)
            ChainedScope(this, "package view scope for $fqName in ${module.getName()}", *scopes.toTypedArray())
        }
    })

    override fun getContainingDeclaration(): PackageViewDescriptor? {
        return if (fqName.isRoot()) null else module.getPackage(fqName.parent())
    }

    override fun equals(other: Any?): Boolean {
        val that = other as? PackageViewDescriptor ?: return false
        return this.fqName == that.fqName && this.module == that.module
    }

    override fun hashCode(): Int {
        var result = module.hashCode()
        result = 31 * result + fqName.hashCode()
        return result
    }

    override fun substitute(substitutor: TypeSubstitutor): DeclarationDescriptor? = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R = visitor.visitPackageViewDescriptor(this, data)
}
