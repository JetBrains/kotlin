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

package org.jetbrains.jet.plugin.refactoring.move

import org.jetbrains.jet.plugin.codeInsight.JetFileReferencesResolver
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.references.JetSimpleNameReference.ShorteningMode
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.plugin.imports.canBeReferencedViaImport
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import java.util.Collections
import org.jetbrains.jet.lang.resolve.name.isImported

public class PackageNameInfo(val oldPackageName: FqName, val newPackageName: FqName)

public fun JetElement.updateInternalReferencesOnPackageNameChange(
        packageNameInfo: PackageNameInfo,
        updateImportedReferences: Boolean,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING
) {
    val file = getContainingFile() as? JetFile
    if (file == null) return

    val importPaths =
            if (updateImportedReferences) {
                file.getImportDirectives().map { it.getImportPath() }.filterNotNull()
            }
            else Collections.emptyList()

    fun isImportedName(fqName: FqName): Boolean =
            updateImportedReferences && importPaths.any { fqName.isImported(it, false) }

    val referenceToContext = JetFileReferencesResolver.resolve(file = file, elements = listOf(this), visitReceivers = false)

    for ((refExpr, bindingContext) in referenceToContext) {
        if (refExpr !is JetSimpleNameExpression) continue

        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.let { descriptor ->
            if (descriptor is ConstructorDescriptor) descriptor.getContainingDeclaration() else descriptor
        }
        if (descriptor == null || !descriptor.canBeReferencedViaImport()) continue

        val declaration = DescriptorToDeclarationUtil.getDeclaration(file, descriptor)
        if (declaration == null || isAncestor(declaration, true)) continue

        val fqName = DescriptorUtils.getFqName(descriptor)
        if (!fqName.isSafe()) continue

        val fqNameSafe = fqName.toSafe()
        val packageName = DescriptorUtils.getParentOfType(
                descriptor, javaClass<PackageFragmentDescriptor>(), false
        )?.let { DescriptorUtils.getFqName(it).toSafe() }

        when {
            packageName == packageNameInfo.oldPackageName,
            packageName == packageNameInfo.newPackageName,
            isImportedName(fqNameSafe) -> {
                (refExpr.getReference() as? JetSimpleNameReference)?.bindToFqName(fqNameSafe, shorteningMode)
            }
        }
    }
}

public fun JetNamedDeclaration.getFileNameAfterMove(): String? {
    return (getContainingFile() as? JetFile)?.let { file ->
        if (file.getDeclarations().size > 1) "${getName()}.${JetFileType.INSTANCE.getDefaultExtension()}" else file.getName()
    }
}