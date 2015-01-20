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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.debugText.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.Importer
import org.jetbrains.kotlin.resolve.ImportsResolver
import org.jetbrains.kotlin.resolve.JetModuleUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.utils.Printer

import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.LookupMode
import java.util.LinkedHashSet
import java.util.HashSet
import com.google.common.collect.ListMultimap
import kotlin.properties.Delegates
import com.google.common.collect.ImmutableListMultimap
import org.jetbrains.kotlin.resolve.scopes.JetScopeSelectorUtil.ScopeByNameSelector
import org.jetbrains.kotlin.resolve.scopes.JetScopeSelectorUtil.ScopeByNameMultiSelector

trait IndexedImports {
    val imports: List<JetImportDirective>
    fun importsForName(name: Name): Collection<JetImportDirective>
}

class AllUnderImportsIndexed(allImports: Collection<JetImportDirective>) : IndexedImports {
    override val imports = allImports.filter { it.isAllUnder() }
    override fun importsForName(name: Name) = imports
}

class AliasImportsIndexed(allImports: Collection<JetImportDirective>) : IndexedImports {
    override val imports = allImports.filter { !it.isAllUnder() }

    private val nameToDirectives: ListMultimap<Name, JetImportDirective> by Delegates.lazy {
        val builder = ImmutableListMultimap.builder<Name, JetImportDirective>()

        for (directive in imports) {
            val path = directive.getImportPath() ?: continue // can be some parse errors
            builder.put(path.getImportedName()!!, directive)
        }

        builder.build()
    }

    override fun importsForName(name: Name) = nameToDirectives.get(name)
}

class LazyImportResolver(
        val resolveSession: ResolveSession,
        val packageView: PackageViewDescriptor,
        val indexedImports: IndexedImports,
        private val traceForImportResolve: BindingTrace,
        includeRootPackageClasses: Boolean
) {
    private val importedScopesProvider = resolveSession.getStorageManager().createMemoizedFunction {
        (directive: JetImportDirective) -> ImportDirectiveResolveCache(directive)
    }
    private val rootScope = JetModuleUtil.getImportsResolutionScope(resolveSession.getModuleDescriptor(), includeRootPackageClasses)

    private var directiveUnderResolve: JetImportDirective? = null

    private class ImportResolveStatus(val lookupMode: LookupMode, val scope: JetScope, val descriptors: Collection<DeclarationDescriptor>)

    private inner class ImportDirectiveResolveCache(private val directive: JetImportDirective) {

        volatile var importResolveStatus: ImportResolveStatus? = null

        fun scopeForMode(mode: LookupMode): JetScope {
            val status = importResolveStatus
            if (status != null && (status.lookupMode == mode || status.lookupMode == LookupMode.EVERYTHING)) {
                return status.scope
            }

            return resolveSession.getStorageManager().compute {
                val cachedStatus = importResolveStatus
                if (cachedStatus != null && (cachedStatus.lookupMode == mode || cachedStatus.lookupMode == LookupMode.EVERYTHING)) {
                    cachedStatus.scope
                }
                else {
                    val directiveImportScope = WritableScopeImpl(JetScope.Empty, packageView, RedeclarationHandler.DO_NOTHING, "Scope for import '" + directive.getDebugText() + "' resolve in " + toString())
                    directiveImportScope.changeLockLevel(WritableScope.LockLevel.BOTH)

                    val importer = Importer()
                    directiveUnderResolve = directive

                    val descriptors: Collection<DeclarationDescriptor>
                    try {
                        val resolver = resolveSession.getQualifiedExpressionResolver()
                        descriptors = resolver.processImportReference(directive, rootScope, packageView.getMemberScope(),
                                                                      importer, traceForImportResolve, mode)
                        importer.doImport(directiveImportScope)
                        if (mode == LookupMode.EVERYTHING) {
                            ImportsResolver.checkPlatformTypesMappedToKotlin(packageView.getModule(), traceForImportResolve, directive, descriptors)
                        }
                    }
                    finally {
                        directiveUnderResolve = null
                        directiveImportScope.changeLockLevel(WritableScope.LockLevel.READING)
                    }

                    importResolveStatus = ImportResolveStatus(mode, directiveImportScope, descriptors)
                    directiveImportScope
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
            val fileScope = resolveSession.getScopeProvider().getFileScope(importDirective.getContainingJetFile())
            ImportsResolver.reportConflictingImport(importDirective, fileScope, status.descriptors, traceForImportResolve)
        }
    }

    public fun <D : DeclarationDescriptor> selectSingleFromImports(
            name: Name,
            lookupMode: LookupMode,
            descriptorSelector: ScopeByNameSelector<D>
    ): D? {
        fun compute(): D? {
            val imports = indexedImports.importsForName(name)
            if (imports.contains(directiveUnderResolve)) {
                // This is the recursion in imports analysis
                return null
            }

            var target: D? = null
            for (directive in imports) {
                val resolved = descriptorSelector.get(getImportScope(directive, lookupMode), name) ?: continue
                if (target != null && target != resolved) return null // ambiguity
                target = resolved
            }
            return target
        }
        return resolveSession.getStorageManager().compute(::compute)
    }

    public fun <D : DeclarationDescriptor> collectFromImports(
            name: Name,
            lookupMode: LookupMode,
            descriptorsSelector: ScopeByNameMultiSelector<D>
    ): Collection<D> {
        return resolveSession.getStorageManager().compute {
            val descriptors = HashSet<D>()
            for (directive in indexedImports.importsForName(name)) {
                if (directive == directiveUnderResolve) {
                    // This is the recursion in imports analysis
                    throw IllegalStateException("Recursion while resolving many imports: " + directive.getText())
                }

                descriptors.addAll(descriptorsSelector.get(getImportScope(directive, lookupMode), name))
            }

            descriptors
        }
    }

    public fun getImportScope(directive: JetImportDirective, lookupMode: LookupMode): JetScope {
        return importedScopesProvider(directive).scopeForMode(lookupMode)
    }

    public fun printScopeStructure(p: Printer) {
        p.print("rootScope = ")
        rootScope.printScopeStructure(p.withholdIndentOnce())
    }
}

class LazyImportScope(
        private val importResolver: LazyImportResolver,
        private val filter: (DeclarationDescriptor) -> Boolean,
        private val debugName: String
) : JetScope {

    private inner class FilteringScopeByNameSelector<D : DeclarationDescriptor>(
            private val selector: ScopeByNameSelector<D>
    ) : ScopeByNameSelector<D> {
        override fun get(scope: JetScope, name: Name): D? {
            val descriptor = selector.get(scope, name)
            return if (descriptor != null && filter(descriptor)) descriptor else null
        }
    }

    private inner class FilteringScopeByNameMultiSelector<D : DeclarationDescriptor>(
            private val selector: ScopeByNameMultiSelector<D>
    ) : ScopeByNameMultiSelector<D> {
        override fun get(scope: JetScope, name: Name): Collection<D> {
            return selector.get(scope, name).filter(filter)
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? {
        return importResolver.selectSingleFromImports(name, LookupMode.ONLY_CLASSES_AND_PACKAGES, FilteringScopeByNameSelector(JetScopeSelectorUtil.CLASSIFIER_DESCRIPTOR_SCOPE_SELECTOR))
    }

    override fun getPackage(name: Name): PackageViewDescriptor? {
        return importResolver.selectSingleFromImports(name, LookupMode.ONLY_CLASSES_AND_PACKAGES, FilteringScopeByNameSelector(JetScopeSelectorUtil.PACKAGE_SCOPE_SELECTOR))
    }

    override fun getProperties(name: Name): Collection<VariableDescriptor> {
        return importResolver.collectFromImports(name, LookupMode.EVERYTHING, FilteringScopeByNameMultiSelector(JetScopeSelectorUtil.NAMED_PROPERTIES_SCOPE_SELECTOR))
    }

    override fun getLocalVariable(name: Name) = null

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
        return importResolver.collectFromImports(name, LookupMode.EVERYTHING, FilteringScopeByNameMultiSelector(JetScopeSelectorUtil.NAMED_FUNCTION_SCOPE_SELECTOR))
    }

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = listOf()

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return importResolver.resolveSession.getStorageManager().compute {
            val descriptors = LinkedHashSet<DeclarationDescriptor>()
            for (directive in importResolver.indexedImports.imports) {
                val importPath = directive.getImportPath() ?: continue
                val importedName = importPath.getImportedName()
                if (importedName == null || nameFilter(importedName)) {
                    importResolver.getImportScope(directive, LookupMode.EVERYTHING).getDescriptors(kindFilter, nameFilter).filterTo(descriptors, filter)
                }
            }
            descriptors
        }
    }

    override fun getImplicitReceiversHierarchy() = listOf<ReceiverParameterDescriptor>()

    override fun getOwnDeclaredDescriptors() = listOf<DeclarationDescriptor>()

    override fun getContainingDeclaration() = importResolver.packageView

    override fun toString() = "LazyImportScope: " + debugName

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " {")
        p.pushIndent()

        p.println("packageDescriptor = ", importResolver.packageView)

        importResolver.printScopeStructure(p)

        p.popIndent()
        p.println("}")
    }
}
