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

package org.jetbrains.jet.plugin.codeInsight

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.plugin.libraries.DecompiledNavigationUtils
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver
import java.util.HashSet
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import com.intellij.psi.JavaPsiFacade
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.plugin.search.allScope

public object DescriptorToDeclarationUtil {
    public fun getDeclaration(file: JetFile, descriptor: DeclarationDescriptor): PsiElement? {
        return getDeclaration(file.getProject(), descriptor)
    }

    public fun getDeclaration(project: Project, descriptor: DeclarationDescriptor): PsiElement? {
        var elements: Collection<PsiElement> = DescriptorToSourceUtils.descriptorToDeclarations(descriptor)
        if (elements.isEmpty()) {
            elements = findDecompiledAndBuiltInDeclarations(project, descriptor)
        }

        return elements.firstOrNull()
    }

    public fun resolveToPsiElements(project: Project, targetDescriptor: DeclarationDescriptor): Collection<PsiElement> {
        val result = HashSet<PsiElement>()

        // todo: remove getOriginal()
        val originalDescriptor = targetDescriptor.getOriginal()
        result.addAll(DescriptorToSourceUtils.descriptorToDeclarations(originalDescriptor))
        result.addAll(DescriptorToDeclarationUtil.findDecompiledAndBuiltInDeclarations(project, originalDescriptor))

        if (originalDescriptor is PackageViewDescriptor) {
            val psiFacade = JavaPsiFacade.getInstance(project)
            val fqName = (originalDescriptor as PackageViewDescriptor).getFqName().asString()
            ContainerUtil.addIfNotNull(result, psiFacade.findPackage(fqName))
            ContainerUtil.addIfNotNull(result, psiFacade.findClass(fqName, project.allScope()))
        }
        return result
    }

    private fun findDecompiledAndBuiltInDeclarations(project: Project, descriptor: DeclarationDescriptor): Collection<PsiElement> {
        val libraryReferenceResolver = project.getComponent(javaClass<BuiltInsReferenceResolver>())
        val elements = libraryReferenceResolver!!.resolveBuiltInSymbol(descriptor)
        if (!elements.isEmpty()) {
            return elements
        }

        val decompiledDeclaration = DecompiledNavigationUtils.findDeclarationForReference(project, descriptor)
        if (decompiledDeclaration != null) {
            return setOf(decompiledDeclaration)
        }
        return setOf()
    }
}