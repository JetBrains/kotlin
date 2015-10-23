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

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.PlatformTypesMappedToKotlinChecker
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import java.util.*

interface IndexedImports {
    val imports: List<KtImportDirective>
    fun importsForName(name: Name): Collection<KtImportDirective>
}

class AllUnderImportsIndexed(allImports: Collection<KtImportDirective>) : IndexedImports {
    override val imports = allImports.filter { it.isAllUnder() }
    override fun importsForName(name: Name) = imports
}

class AliasImportsIndexed(allImports: Collection<KtImportDirective>) : IndexedImports {
    override val imports = allImports.filter { !it.isAllUnder() }

    private val nameToDirectives: ListMultimap<Name, KtImportDirective> by lazy {
        val builder = ImmutableListMultimap.builder<Name, KtImportDirective>()

        for (directive in imports) {
            val path = directive.getImportPath() ?: continue // parse error
            val importedName = path.getImportedName() ?: continue // parse error
            builder.put(importedName, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives.get(name)
}

interface ImportResolver {
    fun forceResolveAllImports()
    fun forceResolveImport(importDirective: KtImportDirective)
}

class LazyImportResolver(
        val storageManager: StorageManager,
        val qualifiedExpressionResolver: QualifiedExpressionResolver,
        val moduleDescriptor: ModuleDescriptor,
        val indexedImports: IndexedImports,
        private val traceForImportResolve: BindingTrace,
        private val packageFragment: PackageFragmentDescriptor
) : ImportResolver {
    private val importedScopesProvider = storageManager.createMemoizedFunctionWithNullableValues {
        directive: KtImportDirective ->
            val directiveImportScope = qualifiedExpressionResolver.processImportReference(
                    directive, moduleDescriptor, traceForImportResolve, packageFragment) ?: return@createMemoizedFunctionWithNullableValues null

            if (!directive.isAllUnder) {
                PlatformTypesMappedToKotlinChecker.checkPlatformTypesMappedToKotlin(
                        moduleDescriptor, traceForImportResolve, directive, directiveImportScope.getAllDescriptors())
            }

            directiveImportScope
    }

    override fun forceResolveAllImports() {
        val explicitClassImports = HashMultimap.create<String, KtImportDirective>()
        for (importDirective in indexedImports.imports) {
            forceResolveImport(importDirective)
            val scope = importedScopesProvider(importDirective)

            val alias = KtPsiUtil.getAliasName(importDirective)?.identifier
            if (scope != null && alias != null) {
                if (scope.getClassifier(Name.identifier(alias), KotlinLookupLocation(importDirective)) != null) {
                    explicitClassImports.put(alias, importDirective)
                }
            }
        }
        for ((alias, import) in explicitClassImports.entries()) {
            if (alias.all { it == '_' }) {
                traceForImportResolve.report(Errors.UNDERSCORE_IS_RESERVED.on(import))
            }
        }
        for (alias in explicitClassImports.keySet()) {
            val imports = explicitClassImports.get(alias)
            if (imports.size() > 1) {
                imports.forEach {
                    traceForImportResolve.report(Errors.CONFLICTING_IMPORT.on(it, alias))
                }
            }
        }
    }

    override fun forceResolveImport(importDirective: KtImportDirective) {
        getImportScope(importDirective)
    }

    public fun <D : DeclarationDescriptor> selectSingleFromImports(
            name: Name,
            descriptorSelector: (KtScope, Name) -> D?
    ): D? {
        fun compute(): D? {
            val imports = indexedImports.importsForName(name)

            var target: D? = null
            for (directive in imports) {
                val resolved = descriptorSelector(getImportScope(directive), name) ?: continue
                if (target != null && target != resolved) return null // ambiguity
                target = resolved
            }
            return target
        }
        return storageManager.compute(::compute)
    }

    public fun <D : DeclarationDescriptor> collectFromImports(
            name: Name,
            descriptorsSelector: (KtScope, Name) -> Collection<D>
    ): Collection<D> {
        return storageManager.compute {
            var descriptors: Collection<D>? = null
            for (directive in indexedImports.importsForName(name)) {
                val descriptorsForImport = descriptorsSelector(getImportScope(directive), name)
                descriptors = descriptors.concat(descriptorsForImport)
            }

            descriptors ?: emptySet<D>()
        }
    }

    public fun getImportScope(directive: KtImportDirective): KtScope {
        return importedScopesProvider(directive) ?: KtScope.Empty
    }
}

class LazyImportScope(
        override val parent: ImportingScope?,
        override val ownerDescriptor: DeclarationDescriptor,
        private val importResolver: LazyImportResolver,
        private val filteringKind: LazyImportScope.FilteringKind,
        private val debugName: String
) : ImportingScope {

    enum class FilteringKind {
        ALL,
        VISIBLE_CLASSES,
        INVISIBLE_CLASSES
    }

    fun isClassVisible(descriptor: ClassDescriptor): Boolean {
        if (filteringKind == FilteringKind.ALL) return true
        val visibility = descriptor.getVisibility()
        val includeVisible = filteringKind == FilteringKind.VISIBLE_CLASSES
        if (!visibility.mustCheckInImports()) return includeVisible
        return Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, descriptor, importResolver.moduleDescriptor) == includeVisible
    }

    override fun getDeclaredClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return importResolver.selectSingleFromImports(name) { scope, name ->
            val descriptor = scope.getClassifier(name, location)
            if (descriptor != null && isClassVisible(descriptor as ClassDescriptor/*no type parameter can be imported*/)) descriptor else null
        }
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return null
        return importResolver.selectSingleFromImports(name) { scope, name -> scope.getPackage(name) }
    }

    override fun getDeclaredVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, name -> scope.getProperties(name, location) }
    }

    override fun getDeclaredFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, name -> scope.getFunctions(name, location) }
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, name -> scope.getSyntheticExtensionProperties(receiverTypes, name, location) }
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name) { scope, name -> scope.getSyntheticExtensionFunctions(receiverTypes, name, location) }
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        return importResolver.storageManager.compute {
            importResolver.indexedImports.imports.flatMapTo(LinkedHashSet<PropertyDescriptor>()) { import ->
                importResolver.getImportScope(import).getSyntheticExtensionProperties(receiverTypes)
            }
        }
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        return importResolver.storageManager.compute {
            importResolver.indexedImports.imports.flatMapTo(LinkedHashSet<FunctionDescriptor>()) { import ->
                importResolver.getImportScope(import).getSyntheticExtensionFunctions(receiverTypes)
            }
        }
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        return importResolver.storageManager.compute {
            val descriptors = LinkedHashSet<DeclarationDescriptor>()
            for (directive in importResolver.indexedImports.imports) {
                val importPath = directive.getImportPath() ?: continue
                val importedName = importPath.getImportedName()
                if (importedName == null || nameFilter(importedName)) {
                    descriptors.addAll(importResolver.getImportScope(directive).getDescriptors(kindFilter, nameFilter))
                }
            }
            descriptors
        }
    }

    override fun toString() = "LazyImportScope: " + debugName

    override fun printStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " {")
        p.pushIndent()

        p.println("ownerDescriptor = ", ownerDescriptor)

        p.popIndent()
        p.println("}")
    }
}
