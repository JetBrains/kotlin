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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.TypeSubstitutor

import java.util.ArrayList

public class PackageViewDescriptorImpl(private val module: ModuleDescriptor, private val fqName: FqName, fragments: List<PackageFragmentDescriptor>) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {
    private val memberScope: JetScope

    init {

        val scopes = ArrayList<JetScope>(fragments.size() + 1)
        assert(!fragments.isEmpty()) { fqName + " in " + module }
        for (fragment in fragments) {
            scopes.add(fragment.getMemberScope())
        }
        scopes.add(SubpackagesScope(this))

        memberScope = ChainedScope(this, "package view scope for " + fqName + " in " + module.getName(), *scopes.toArray<JetScope>(arrayOfNulls<JetScope>(scopes.size())))
    }

    override fun getContainingDeclaration(): PackageViewDescriptor? {
        return if (fqName.isRoot()) null else module.getPackage(fqName.parent())
    }

    override fun substitute(substitutor: TypeSubstitutor): DeclarationDescriptor? {
        return this
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitPackageViewDescriptor(this, data)
    }

    override fun getFqName(): FqName {
        return fqName
    }

    override fun getMemberScope(): JetScope {
        return memberScope
    }

    override fun getModule(): ModuleDescriptor {
        return module
    }

    override fun equals(o: Any?): Boolean {
        if (this == o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as PackageViewDescriptorImpl

        if (fqName != that.fqName) return false
        if (module != that.module) return false

        return true
    }


    override fun hashCode(): Int {
        var result = module.hashCode()
        result = 31 * result + fqName.hashCode()
        return result
    }
}
