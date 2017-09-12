/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.Printer

class IdeSampleResolutionService(val project: Project) : SampleResolutionService {

    override fun resolveSample(context: BindingContext, fromDescriptor: DeclarationDescriptor, resolutionFacade: ResolutionFacade, qualifiedName: List<String>): Collection<DeclarationDescriptor> {

        val scope = KotlinSourceFilterScope.projectAndLibrariesSources(GlobalSearchScope.projectScope(project), project)

        val shortName = qualifiedName.lastOrNull() ?: return emptyList()

        val targetFqName = FqName.fromSegments(qualifiedName)

        val functions = KotlinFunctionShortNameIndex.getInstance().get(shortName, project, scope).asSequence()
        val classes = KotlinClassShortNameIndex.getInstance().get(shortName, project, scope).asSequence()

        val descriptors = (functions + classes)
                .filter { it.fqName == targetFqName }
                .map { it.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) } // TODO Filter out not visible due dependencies config descriptors
                .toList()
        if (descriptors.isNotEmpty())
            return descriptors

        if (!targetFqName.isRoot && PackageIndexUtil.packageExists(targetFqName, scope, project))
            return listOf(GlobalSyntheticPackageViewDescriptor(targetFqName, project, scope))
        return emptyList()
    }
}

private fun shouldNotBeCalled(): Nothing = throw UnsupportedOperationException("Synthetic PVD for KDoc link resolution")

private class GlobalSyntheticPackageViewDescriptor(override val fqName: FqName, private val project: Project, private val scope: GlobalSearchScope) : PackageViewDescriptor {
    override fun getContainingDeclaration(): PackageViewDescriptor? =
            if (fqName.isOneSegmentFQN()) null else GlobalSyntheticPackageViewDescriptor(fqName.parent(), project, scope)


    override val memberScope: MemberScope = object : MemberScope {

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = shouldNotBeCalled()

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> = shouldNotBeCalled()

        override fun getFunctionNames(): Set<Name> = shouldNotBeCalled()

        override fun getVariableNames(): Set<Name> = shouldNotBeCalled()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = shouldNotBeCalled()

        override fun printScopeStructure(p: Printer) {
            p.printIndent()
            p.print("GlobalSyntheticPackageViewDescriptorMemberScope (INDEX)")
        }


        fun getClassesByNameFilter(nameFilter: (Name) -> Boolean) = KotlinFullClassNameIndex.getInstance()
                .getAllKeys(project)
                .asSequence()
                .filter { it.startsWith(fqName.asString()) }
                .map(::FqName)
                .filter { it.isChildOf(fqName) }
                .filter { nameFilter(it.shortName()) }
                .flatMap { KotlinFullClassNameIndex.getInstance()[it.asString(), project, scope].asSequence() }
                .map { it.resolveToDescriptorIfAny() }

        fun getFunctionsByNameFilter(nameFilter: (Name) -> Boolean) = KotlinTopLevelFunctionFqnNameIndex.getInstance()
                .getAllKeys(project)
                .asSequence()
                .filter { it.startsWith(fqName.asString()) }
                .map(::FqName)
                .filter { it.isChildOf(fqName) }
                .filter { nameFilter(it.shortName()) }
                .flatMap { KotlinTopLevelFunctionFqnNameIndex.getInstance()[it.asString(), project, scope].asSequence() }
                .map { it.resolveToDescriptorIfAny() }

        fun getSubpackages(nameFilter: (Name) -> Boolean) =
                PackageIndexUtil.getSubPackageFqNames(fqName, scope, project, nameFilter)
                        .map { GlobalSyntheticPackageViewDescriptor(it, project, scope) }

        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor>
                = (getClassesByNameFilter(nameFilter)
                   + getFunctionsByNameFilter(nameFilter)
                   + getSubpackages(nameFilter)).filterNotNull().toList()

    }
    override val module: ModuleDescriptor
        get() = shouldNotBeCalled()
    override val fragments: List<PackageFragmentDescriptor>
        get() = shouldNotBeCalled()

    override fun getOriginal() = this

    override fun getName(): Name = fqName.shortName()

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R = shouldNotBeCalled()

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = shouldNotBeCalled()

    override val annotations = Annotations.EMPTY
}