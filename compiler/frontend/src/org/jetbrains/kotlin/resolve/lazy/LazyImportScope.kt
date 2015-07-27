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

import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.PlatformTypesMappedToKotlinChecker
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.UsageLocation
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.util.collectionUtils.concat
import org.jetbrains.kotlin.utils.Printer
import java.util.LinkedHashSet

interface IndexedImports {
    val imports: List<JetImportDirective>
    fun importsForName(name: Name): Collection<JetImportDirective>
}

class AllUnderImportsIndexed(allImports: Collection<JetImportDirective>) : IndexedImports {
    override val imports = allImports.filter { it.isAllUnder() }
    override fun importsForName(name: Name) = imports
}

class AliasImportsIndexed(allImports: Collection<JetImportDirective>) : IndexedImports {
    override val imports = allImports.filter { !it.isAllUnder() }

    private val nameToDirectives: ListMultimap<Name, JetImportDirective> by lazy {
        val builder = ImmutableListMultimap.builder<Name, JetImportDirective>()

        for (directive in imports) {
            val path = directive.getImportPath() ?: continue // parse error
            val importedName = path.getImportedName() ?: continue // parse error
            builder.put(importedName, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives.get(name)
}

class LazyImportResolver(
        val storageManager: StorageManager,
        val qualifiedExpressionResolver: QualifiedExpressionResolver,
        val fileScopeProvider: FileScopeProvider,
        val moduleDescriptor: ModuleDescriptor,
        val indexedImports: IndexedImports,
        private val traceForImportResolve: BindingTrace
) {
    private val importedScopesProvider = storageManager.createMemoizedFunction {
        directive: JetImportDirective -> ImportDirectiveResolveCache(directive)
    }

    private var directiveUnderResolve: JetImportDirective? = null

    private class ImportResolveStatus(val lookupMode: LookupMode, val scope: JetScope, val descriptors: Collection<DeclarationDescriptor>)

    private inner class ImportDirectiveResolveCache(private val directive: JetImportDirective) {

        volatile var importResolveStatus: ImportResolveStatus? = null

        fun scopeForMode(mode: LookupMode): JetScope {
            val status = importResolveStatus
            if (status != null && (status.lookupMode == mode || status.lookupMode == LookupMode.EVERYTHING)) {
                return status.scope
            }

            return storageManager.compute {
                val cachedStatus = importResolveStatus
                if (cachedStatus != null && (cachedStatus.lookupMode == mode || cachedStatus.lookupMode == LookupMode.EVERYTHING)) {
                    cachedStatus.scope
                }
                else {
                    directiveUnderResolve = directive

                    try {
                        val directiveImportScope = qualifiedExpressionResolver.processImportReference(
                                directive, moduleDescriptor, traceForImportResolve, mode)
                        val descriptors = if (directive.isAllUnder()) emptyList() else directiveImportScope.getAllDescriptors()

                        if (mode == LookupMode.EVERYTHING) {
                            PlatformTypesMappedToKotlinChecker.checkPlatformTypesMappedToKotlin(moduleDescriptor, traceForImportResolve, directive, descriptors)
                        }

                        importResolveStatus = ImportResolveStatus(mode, directiveImportScope, descriptors)
                        directiveImportScope
                    }
                    finally {
                        directiveUnderResolve = null
                    }
                }
            }
        }
    }

    public fun forceResolveAllContents() {
        for (importDirective in indexedImports.imports) {
            forceResolveImportDirective(importDirective)
        }
    }

    public fun forceResolveImportDirective(importDirective: JetImportDirective) {
        getImportScope(importDirective, LookupMode.EVERYTHING)

        val status = importedScopesProvider(importDirective).importResolveStatus
        if (status != null && !status.descriptors.isEmpty()) {
            val fileScope = fileScopeProvider.getFileScope(importDirective.getContainingJetFile())
            reportConflictingImport(importDirective, fileScope, status.descriptors, traceForImportResolve)
        }
    }

    private fun reportConflictingImport(
            importDirective: JetImportDirective,
            fileScope: JetScope,
            resolvedTo: Collection<DeclarationDescriptor>?,
            trace: BindingTrace
    ) {

        val importedReference = importDirective.getImportedReference()
        if (importedReference == null || resolvedTo == null) return

        val aliasName = JetPsiUtil.getAliasName(importDirective) ?: return

        if (resolvedTo.size() != 1) return

        when (resolvedTo.single()) {
            is ClassDescriptor -> {
                if (fileScope.getClassifier(aliasName) == null) {
                    trace.report(Errors.CONFLICTING_IMPORT.on(importedReference, aliasName.asString()))
                }
            }
            is PackageViewDescriptor -> {
                if (fileScope.getPackage(aliasName) == null) {
                    trace.report(Errors.CONFLICTING_IMPORT.on(importedReference, aliasName.asString()))
                }
            }
        }
    }


    public fun <D : DeclarationDescriptor> selectSingleFromImports(
            name: Name,
            lookupMode: LookupMode,
            descriptorSelector: (JetScope, Name) -> D?
    ): D? {
        fun compute(): D? {
            val imports = indexedImports.importsForName(name)
            if (imports.contains(directiveUnderResolve)) {
                // This is the recursion in imports analysis
                return null
            }

            var target: D? = null
            for (directive in imports) {
                val resolved = descriptorSelector(getImportScope(directive, lookupMode), name) ?: continue
                if (target != null && target != resolved) return null // ambiguity
                target = resolved
            }
            return target
        }
        return storageManager.compute(::compute)
    }

    public fun <D : DeclarationDescriptor> collectFromImports(
            name: Name,
            lookupMode: LookupMode,
            descriptorsSelector: (JetScope, Name) -> Collection<D>
    ): Collection<D> {
        return storageManager.compute {
            var descriptors: Collection<D>? = null
            for (directive in indexedImports.importsForName(name)) {
                if (directive == directiveUnderResolve) {
                    // This is the recursion in imports analysis
                    throw IllegalStateException("Recursion while resolving many imports: " + directive.getText())
                }

                val descriptorsForImport = descriptorsSelector(getImportScope(directive, lookupMode), name)
                descriptors = descriptors.concat(descriptorsForImport)
            }

            descriptors ?: emptySet<D>()
        }
    }

    public fun getImportScope(directive: JetImportDirective, lookupMode: LookupMode): JetScope {
        return importedScopesProvider(directive).scopeForMode(lookupMode)
    }
}

class LazyImportScope(
        private val containingDeclaration: DeclarationDescriptor,
        private val importResolver: LazyImportResolver,
        private val filteringKind: LazyImportScope.FilteringKind,
        private val debugName: String
) : JetScope {

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

    override fun getClassifier(name: Name, location: UsageLocation): ClassifierDescriptor? {
        return importResolver.selectSingleFromImports(name, LookupMode.ONLY_CLASSES_AND_PACKAGES) { scope, name ->
            val descriptor = scope.getClassifier(name, location)
            if (descriptor != null && isClassVisible(descriptor as ClassDescriptor/*no type parameter can be imported*/)) descriptor else null
        }
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return null
        return importResolver.selectSingleFromImports(name, LookupMode.ONLY_CLASSES_AND_PACKAGES) { scope, name -> scope.getPackage(name) }
    }

    override fun getProperties(name: Name, location: UsageLocation): Collection<VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name, LookupMode.EVERYTHING) { scope, name -> scope.getProperties(name, location) }
    }

    override fun getLocalVariable(name: Name) = null

    override fun getFunctions(name: Name, location: UsageLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name, LookupMode.EVERYTHING) { scope, name -> scope.getFunctions(name, location) }
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>, name: Name): Collection<PropertyDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name, LookupMode.EVERYTHING) { scope, name -> scope.getSyntheticExtensionProperties(receiverTypes, name) }
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()
        return importResolver.collectFromImports(name, LookupMode.EVERYTHING) { scope, name -> scope.getSyntheticExtensionFunctions(receiverTypes, name) }
    }

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<JetType>): Collection<PropertyDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        return importResolver.storageManager.compute {
            importResolver.indexedImports.imports.flatMapTo(LinkedHashSet<PropertyDescriptor>()) { import ->
                importResolver.getImportScope(import, LookupMode.EVERYTHING).getSyntheticExtensionProperties(receiverTypes)
            }
        }
    }

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = listOf()

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        // we do not perform any filtering by visibility here because all descriptors from both visible/invisible filter scopes are to be added anyway
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return listOf()

        return importResolver.storageManager.compute {
            val descriptors = LinkedHashSet<DeclarationDescriptor>()
            for (directive in importResolver.indexedImports.imports) {
                val importPath = directive.getImportPath() ?: continue
                val importedName = importPath.getImportedName()
                if (importedName == null || nameFilter(importedName)) {
                    descriptors.addAll(importResolver.getImportScope(directive, LookupMode.EVERYTHING).getDescriptors(kindFilter, nameFilter))
                }
            }
            descriptors
        }
    }

    override fun getImplicitReceiversHierarchy() = listOf<ReceiverParameterDescriptor>()

    override fun getOwnDeclaredDescriptors() = listOf<DeclarationDescriptor>()

    override fun getContainingDeclaration() = containingDeclaration

    override fun toString() = "LazyImportScope: " + debugName

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " {")
        p.pushIndent()

        p.println("containingDeclaration = ", containingDeclaration)

        p.popIndent()
        p.println("}")
    }
}
