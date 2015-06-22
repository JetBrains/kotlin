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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.psi.search.FilenameIndex
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.module.impl.scopes.LibraryScope
import com.intellij.openapi.roots.LibraryOrderEntry
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.module.impl.scopes.JdkScope
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

private val LOG = Logger.getInstance("org.jetbrains.kotlin.idea.debugger")

/**
 * This method finds class name for top-level declaration in source file attached to library.
 * The problem is that package-part class file has hash in className and it depends on machine where library was built,
 * so we couldn't predict it.
 * 1. find all .class files with package-part prefix, if there is one - return it
 * 2. find all descriptors with same signature, if there is one - return it
 * 3. else -> return null, because it means that there is more than one function with same signature in project
 */
fun findPackagePartInternalNameForLibraryFile(topLevelDeclaration: JetDeclaration): String? {
    val packagePartFile = findPackagePartFileNamesForElement(topLevelDeclaration).singleOrNull()
    if (packagePartFile != null) return packagePartFile


    val descriptor = topLevelDeclaration.resolveToDescriptor()
    if (descriptor !is CallableDescriptor) return null

    val packageFqName = topLevelDeclaration.getContainingJetFile().getPackageFqName()
    val packageDescriptor = descriptor.module.getPackage(packageFqName)
    if (packageDescriptor == null) {
        reportError(topLevelDeclaration, descriptor)
        return null
    }

    val descFromSourceText = render(descriptor)

    val descriptors: Collection<CallableDescriptor> = when (descriptor) {
        is FunctionDescriptor -> packageDescriptor.memberScope.getFunctions(descriptor.getName())
        is PropertyDescriptor -> packageDescriptor.memberScope.getProperties(descriptor.getName())
        else -> {
            reportError(topLevelDeclaration, descriptor)
            listOf()
        }
    }

    val deserializedDescriptor = descriptors
            .filterIsInstance<DeserializedCallableMemberDescriptor>()
            .filter { render(it) == descFromSourceText }
            .singleOrNull()

    if (deserializedDescriptor == null) {
        reportError(topLevelDeclaration, descriptor)
        return null
    }

    val proto = deserializedDescriptor.proto
    if (proto.hasExtension(JvmProtoBuf.implClassName)) {
        val name = deserializedDescriptor.nameResolver.getName(proto.getExtension(JvmProtoBuf.implClassName)!!)
        return JvmClassName.byFqNameWithoutInnerClasses(packageFqName.child(name)).getInternalName()
    }

    return null
}

private fun findPackagePartFileNamesForElement(elementAt: JetElement): List<String> {
    val project = elementAt.getProject()
    val file = elementAt.getContainingJetFile()

    val packagePartName = PackagePartClassUtils.getPackagePartFqName(file).shortName().asString()
    val packagePartNameWoHash = packagePartName.substring(0, packagePartName.lastIndexOf("$") + 1)

    val libraryEntry = LibraryUtil.findLibraryEntry(file.getVirtualFile(), project)
    val scope = if (libraryEntry is LibraryOrderEntry){
                    LibraryScope(project, libraryEntry.getLibrary() ?: throw AssertionError("Cannot find library for file ${file.getVirtualFile()?.getPath()}"))
                }
                else {
                    JdkScope(project, libraryEntry as JdkOrderEntry)
                }

    val packagePartFiles = FilenameIndex.getAllFilenames(project).sequence().filter {
                it.startsWith(packagePartNameWoHash) && it.endsWith(".class") &&
                    !it.substringAfter(packagePartNameWoHash).contains("\$")
            }.flatMap {
                FilenameIndex.getVirtualFilesByName(project, it, scope).sequence()
            }.map {
                val packageFqName = file.getPackageFqName()
                if (packageFqName.isRoot()) {
                    it.getNameWithoutExtension()
                } else {
                    "${packageFqName.asString()}.${it.getNameWithoutExtension()}"
                }
            }
    return packagePartFiles.toList()
}

private fun render(desc: DeclarationDescriptor) = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(desc)

private fun reportError(element: JetElement, descriptor: CallableDescriptor?) {
    LOG.error("Couldn't calculate class name for element in library scope:\n" +
              element.getElementTextWithContext() +
              if (descriptor != null) "\ndescriptor = ${render(descriptor)}" else ""
    )
}
