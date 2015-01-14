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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.resolve.scopes.FilteringScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import java.util.ArrayList

public trait Importer {
    public fun addAllUnderImport(descriptor: DeclarationDescriptor)
    public fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name)

    public open class StandardImporter(
            private val fileScope: WritableScope,
            private val platformToKotlinClassMap: PlatformToKotlinClassMap
    ) : Importer {

        override fun addAllUnderImport(descriptor: DeclarationDescriptor) {
            importAllUnderDeclaration(descriptor)
        }

        override fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name) {
            importDeclarationAlias(descriptor, aliasName)
        }

        protected fun importDeclarationAlias(descriptor: DeclarationDescriptor, aliasName: Name) {
            when (descriptor) {
                is ClassifierDescriptor -> fileScope.importClassifierAlias(aliasName, descriptor)
                is PackageViewDescriptor -> fileScope.importPackageAlias(aliasName, descriptor)
                is FunctionDescriptor -> fileScope.importFunctionAlias(aliasName, descriptor)
                is VariableDescriptor -> fileScope.importVariableAlias(aliasName, descriptor)
                else -> error("Unknown descriptor")
            }
        }

        protected fun importAllUnderDeclaration(descriptor: DeclarationDescriptor) {
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
            return FilteringScope(scope) { descriptor -> kotlinAnalogsForClassesInside.all { it.getName() != descriptor.getName() } }
        }
    }

    public class DelayedImporter(
            fileScope: WritableScope,
            platformToKotlinClassMap: PlatformToKotlinClassMap
    ) : StandardImporter(fileScope, platformToKotlinClassMap) {

        private trait DelayedImportEntry

        private class AllUnderImportEntry(val descriptor: DeclarationDescriptor) : DelayedImportEntry

        private class AliasImportEntry(val descriptor: DeclarationDescriptor, val name: Name) : DelayedImportEntry

        private val imports = ArrayList<DelayedImportEntry>()

        override fun addAllUnderImport(descriptor: DeclarationDescriptor) {
            imports.add(AllUnderImportEntry(descriptor))
        }

        override fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name) {
            imports.add(AliasImportEntry(descriptor, aliasName))
        }

        public fun processImports() {
            for (anImport in imports) {
                if (anImport is AllUnderImportEntry) {
                    importAllUnderDeclaration(anImport.descriptor)
                }
                else {
                    anImport as AliasImportEntry
                    importDeclarationAlias(anImport.descriptor, anImport.name)
                }
            }
        }
    }

    object DoNothingImporter : Importer {
        override fun addAllUnderImport(descriptor: DeclarationDescriptor) {
        }

        override fun addAliasImport(descriptor: DeclarationDescriptor, aliasName: Name) {
        }
    }
}
