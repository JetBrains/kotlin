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

package org.jetbrains.jet.lang.resolve.lazy

import com.google.common.collect.Sets
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.lang.psi.debugText.*
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.Importer
import org.jetbrains.jet.lang.resolve.ImportsResolver
import org.jetbrains.jet.lang.resolve.JetModuleUtil
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.*
import org.jetbrains.jet.utils.Printer

import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode
import java.util.LinkedHashSet

public class LazyImportScope(private val resolveSession: ResolveSession,
                             private val containingDeclaration: PackageViewDescriptor,
                             imports: List<JetImportDirective>,
                             private val traceForImportResolve: BindingTrace,
                             private val debugName: String,
                             inRootPackage: Boolean) : JetScope, LazyEntity {

    private val importsProvider = ImportsProvider(resolveSession.getStorageManager(), imports)
    private val importedScopesProvider = resolveSession.getStorageManager().createMemoizedFunction {
        (directive: JetImportDirective) -> ImportDirectiveResolveCache(directive)
    }
    private val rootScope = JetModuleUtil.getImportsResolutionScope(resolveSession.getModuleDescriptor(), inRootPackage)

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
                    val directiveImportScope = WritableScopeImpl(JetScope.Empty, containingDeclaration, RedeclarationHandler.DO_NOTHING, "Scope for import '" + directive.getDebugText() + "' resolve in " + toString())
                    directiveImportScope.changeLockLevel(WritableScope.LockLevel.BOTH)

                    val importer = Importer.StandardImporter(directiveImportScope)
                    directiveUnderResolve = directive

                    val descriptors: Collection<DeclarationDescriptor>
                    try {
                        val resolver = resolveSession.getQualifiedExpressionResolver()
                        descriptors = resolver.processImportReference(directive, rootScope, containingDeclaration.getMemberScope(),
                                                                      importer, traceForImportResolve, resolveSession.getModuleDescriptor(), mode)
                        if (mode == LookupMode.EVERYTHING) {
                            ImportsResolver.checkPlatformTypesMappedToKotlin(containingDeclaration.getModule(), traceForImportResolve, directive, descriptors)
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

    override fun forceResolveAllContents() {
        for (importDirective in importsProvider.getAllImports()) {
            forceResolveImportDirective(importDirective)
        }
    }

    public fun forceResolveImportDirective(importDirective: JetImportDirective) {
        getImportScope(importDirective, LookupMode.EVERYTHING)

        val status = importedScopesProvider(importDirective).importResolveStatus
        if (status != null && !status.descriptors.isEmpty()) {
            val fileScope = resolveSession.getScopeProvider().getFileScope(importDirective.getContainingJetFile())
            ImportsResolver.reportUselessImport(importDirective, fileScope, status.descriptors, traceForImportResolve)
        }
    }

    private fun <D : DeclarationDescriptor> selectFirstFromImports(name: Name, lookupMode: LookupMode, descriptorSelector: JetScopeSelectorUtil.ScopeByNameSelector<D>): D? {
        fun compute(): D? {
            for (directive in importsProvider.getImports(name)) {
                if (directive == directiveUnderResolve) {
                    // This is the recursion in imports analysis
                    return null
                }

                val foundDescriptor = descriptorSelector.get(getImportScope(directive, lookupMode), name)
                if (foundDescriptor != null) {
                    return foundDescriptor
                }
            }

            return null
        }
        return resolveSession.getStorageManager().compute(::compute)
    }

    private fun <D : DeclarationDescriptor> collectFromImports(name: Name, lookupMode: LookupMode, descriptorsSelector: JetScopeSelectorUtil.ScopeByNameMultiSelector<D>): Collection<D> {
        return resolveSession.getStorageManager().compute {
            val descriptors = Sets.newHashSet<D>()
            for (directive in importsProvider.getImports(name)) {
                if (directive == directiveUnderResolve) {
                    // This is the recursion in imports analysis
                    throw IllegalStateException("Recursion while resolving many imports: " + directive.getText())
                }

                descriptors.addAll(descriptorsSelector.get(getImportScope(directive, lookupMode), name))
            }

            descriptors
        }
    }

    private fun getImportScope(directive: JetImportDirective, lookupMode: LookupMode) = importedScopesProvider(directive).scopeForMode(lookupMode)

    override fun getClassifier(name: Name) = selectFirstFromImports(name, LookupMode.ONLY_CLASSES_AND_PACKAGES, JetScopeSelectorUtil.CLASSIFIER_DESCRIPTOR_SCOPE_SELECTOR)

    override fun getPackage(name: Name) = selectFirstFromImports(name, LookupMode.ONLY_CLASSES_AND_PACKAGES, JetScopeSelectorUtil.PACKAGE_SCOPE_SELECTOR)

    override fun getProperties(name: Name) = collectFromImports(name, LookupMode.EVERYTHING, JetScopeSelectorUtil.NAMED_PROPERTIES_SCOPE_SELECTOR)

    override fun getLocalVariable(name: Name) = null

    override fun getFunctions(name: Name) = collectFromImports(name, LookupMode.EVERYTHING, JetScopeSelectorUtil.NAMED_FUNCTION_SCOPE_SELECTOR)

    override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = listOf()

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return resolveSession.getStorageManager().compute {
            val descriptors = LinkedHashSet<DeclarationDescriptor>()
            for (directive in importsProvider.getAllImports()) {
                if (directive == directiveUnderResolve) {
                    // This is the recursion in imports analysis
                    throw IllegalStateException("Recursion while resolving many imports: " + directive.getText())
                }

                val importPath = directive.getImportPath() ?: continue
                val importedName = importPath.getImportedName()
                if (importedName == null || nameFilter(importedName)) {
                    descriptors.addAll(getImportScope(directive, LookupMode.EVERYTHING).getDescriptors(kindFilter, nameFilter))
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

        p.println("packageDescriptor = ", containingDeclaration)

        p.print("rootScope = ")
        rootScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

    class object {
        public fun createImportScopeForFile(resolveSession: ResolveSession,
                                            packageDescriptor: PackageViewDescriptor,
                                            jetFile: JetFile,
                                            traceForImportResolve: BindingTrace,
                                            debugName: String): LazyImportScope {
            val importDirectives: List<JetImportDirective> = if (jetFile is JetCodeFragment) {
                jetFile.importsAsImportList()?.getImports() ?: listOf()
            }
            else {
                jetFile.getImportDirectives()
            }

            return LazyImportScope(resolveSession, packageDescriptor, importDirectives.reverse(), traceForImportResolve, debugName, packageDescriptor.getFqName().isRoot())
        }
    }
}
