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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.OldPackageFacadeClassUtils.getPackageClassId
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptPackageFragment
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

object DecompiledNavigationUtils {

    fun getDeclarationFromDecompiledClassFile(
            project: Project,
            referencedDescriptor: DeclarationDescriptor): KtDeclaration? {
        if (isLocal(referencedDescriptor)) return null

        val virtualFile = findVirtualFileContainingDescriptor(project, referencedDescriptor) ?: return null

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
        if (psiFile !is KtDecompiledFile) {
            return null
        }

        return psiFile.getDeclarationForDescriptor(referencedDescriptor)
    }

    private fun isLocal(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is ParameterDescriptor) {
            return isLocal(descriptor.containingDeclaration)
        }
        else {
            return DescriptorUtils.isLocal(descriptor)
        }
    }

    /*
        Find virtual file which contains the declaration of descriptor we're navigating to.
     */
    private fun findVirtualFileContainingDescriptor(
            project: Project,
            referencedDescriptor: DeclarationDescriptor): VirtualFile? {
        if (ErrorUtils.isError(referencedDescriptor)) return null

        val containerClassId = getContainerClassId(project, referencedDescriptor) ?: return null

        val scopeToSearchIn = KotlinSourceFilterScope.sourceAndClassFiles(GlobalSearchScope.allScope(project), project)

        val virtualFileFinderFactory: VirtualFileFinderFactory
        if (isFromKotlinJavasriptMetadata(referencedDescriptor)) {
            virtualFileFinderFactory = JsVirtualFileFinderFactory.SERVICE.getInstance(project)
        }
        else {
            virtualFileFinderFactory = JvmVirtualFileFinderFactory.SERVICE.getInstance(project)
        }

        val fileFinder = virtualFileFinderFactory.create(scopeToSearchIn)
        return fileFinder.findVirtualFileWithHeader(containerClassId)
    }

    private fun isFromKotlinJavasriptMetadata(referencedDescriptor: DeclarationDescriptor): Boolean {
        val packageFragmentDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, PackageFragmentDescriptor::class.java, false)
        return packageFragmentDescriptor is KotlinJavascriptPackageFragment
    }

    //TODO: navigate to inner classes
    //TODO: should we construct proper SourceElement's for decompiled parts / facades?
    private fun getContainerClassId(project: Project, referencedDescriptor: DeclarationDescriptor): ClassId? {
        val deserializedCallableContainer = DescriptorUtils.getParentOfType(referencedDescriptor, DeserializedCallableMemberDescriptor::class.java, true)
        if (deserializedCallableContainer != null) {
            return getContainerClassId(project, deserializedCallableContainer)
        }

        val containerDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, ClassOrPackageFragmentDescriptor::class.java, false)
        if (containerDescriptor is PackageFragmentDescriptor) {
            val packageFQN = containerDescriptor.fqName

            if (referencedDescriptor is DeserializedCallableMemberDescriptor) {
                val partClassName = JvmFileClassUtil.getImplClassName(referencedDescriptor)
                if (partClassName != null) {
                    val partFQN = packageFQN.child(partClassName)
                    val multifileFacadeJetFiles = StaticFacadeIndexUtil.getMultifileClassForPart(partFQN, GlobalSearchScope.allScope(project), project)
                    if (multifileFacadeJetFiles.isEmpty()) {
                        return ClassId(packageFQN, partClassName)
                    }
                    else {
                        val multifileFacade = multifileFacadeJetFiles.iterator().next()
                        val multifileFacadeName = multifileFacade.virtualFile.nameWithoutExtension
                        return ClassId(packageFQN, Name.identifier(multifileFacadeName))
                    }
                }
            }

            return getPackageClassId(packageFQN)
        }
        if (containerDescriptor is ClassDescriptor) {
            if (containerDescriptor.containingDeclaration is ClassDescriptor || ExpressionTypingUtils.isLocal(containerDescriptor.containingDeclaration, containerDescriptor)) {
                return getContainerClassId(project, containerDescriptor.containingDeclaration)
            }
            return containerDescriptor.classId
        }
        return null
    }
}
