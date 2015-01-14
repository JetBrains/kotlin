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

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Lists
import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.resolve.scopes.FilteringScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.WritableScope

public trait Importer {
    public fun addAllUnderImport(descriptor: DeclarationDescriptor, platformToKotlinClassMap: PlatformToKotlinClassMap)

    public fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name)

    public open class StandardImporter(private val fileScope: WritableScope) : Importer {

        override fun addAllUnderImport(descriptor: DeclarationDescriptor, platformToKotlinClassMap: PlatformToKotlinClassMap) {
            importAllUnderDeclaration(descriptor, platformToKotlinClassMap)
        }

        override fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name) {
            importDeclarationAlias(descriptor, aliasName)
        }

        protected fun importDeclarationAlias(descriptor: DeclarationDescriptor, aliasName: Name) {
            if (descriptor is ClassifierDescriptor) {
                fileScope.importClassifierAlias(aliasName, descriptor)
            }
            else if (descriptor is PackageViewDescriptor) {
                fileScope.importPackageAlias(aliasName, descriptor)
            }
            else if (descriptor is FunctionDescriptor) {
                fileScope.importFunctionAlias(aliasName, descriptor)
            }
            else if (descriptor is VariableDescriptor) {
                fileScope.importVariableAlias(aliasName, descriptor)
            }
        }

        protected fun importAllUnderDeclaration(descriptor: DeclarationDescriptor, platformToKotlinClassMap: PlatformToKotlinClassMap) {
            if (descriptor is PackageViewDescriptor) {
                val scope = NoSubpackagesInPackageScope(descriptor)
                fileScope.importScope(createFilteringScope(scope, descriptor, platformToKotlinClassMap))
            }
            else if (descriptor is ClassDescriptor && descriptor.getKind() != ClassKind.OBJECT) {
                fileScope.importScope(descriptor.getStaticScope())
                fileScope.importScope(descriptor.getUnsubstitutedInnerClassesScope())
                val classObjectDescriptor = descriptor.getClassObjectDescriptor()
                if (classObjectDescriptor != null) {
                    fileScope.importScope(classObjectDescriptor.getUnsubstitutedInnerClassesScope())
                }
            }
        }

        private fun createFilteringScope(scope: JetScope, descriptor: PackageViewDescriptor, platformToKotlinClassMap: PlatformToKotlinClassMap): JetScope {
            val kotlinAnalogsForClassesInside = platformToKotlinClassMap.mapPlatformClassesInside(descriptor)
            if (kotlinAnalogsForClassesInside.isEmpty()) return scope
            return FilteringScope(scope) { descriptor -> !kotlinAnalogsForClassesInside.any { it.getName() == descriptor.getName() } }
        }
    }

    public class DelayedImporter(fileScope: WritableScope) : StandardImporter(fileScope) {
        private trait DelayedImportEntry
        private class AllUnderImportEntry(first: DeclarationDescriptor, second: PlatformToKotlinClassMap?) : Pair<DeclarationDescriptor, PlatformToKotlinClassMap>(first, second), DelayedImportEntry
        private class AliasImportEntry(first: DeclarationDescriptor, second: Name) : Pair<DeclarationDescriptor, Name>(first, second), DelayedImportEntry

        private val imports = Lists.newArrayList<DelayedImportEntry>()

        override fun addAllUnderImport(descriptor: DeclarationDescriptor, platformToKotlinClassMap: PlatformToKotlinClassMap) {
            imports.add(AllUnderImportEntry(descriptor, platformToKotlinClassMap))
        }

        override fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name) {
            imports.add(AliasImportEntry(descriptor, aliasName))
        }

        public fun processImports() {
            for (anImport in imports) {
                if (anImport is AllUnderImportEntry) {
                    val allUnderImportEntry = anImport as AllUnderImportEntry
                    importAllUnderDeclaration(allUnderImportEntry.getFirst(), allUnderImportEntry.getSecond())
                }
                else {
                    val aliasImportEntry = anImport as AliasImportEntry
                    importDeclarationAlias(aliasImportEntry.getFirst(), aliasImportEntry.getSecond())
                }
            }
        }
    }

    class object {
        public val DO_NOTHING: Importer = object : Importer {
            override fun addAllUnderImport(descriptor: DeclarationDescriptor, platformToKotlinClassMap: PlatformToKotlinClassMap) {
            }

            override fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name) {
            }
        }
    }
}
