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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.decompiler.navigation.DecompiledNavigationUtils
import org.jetbrains.kotlin.idea.references.BuiltInsReferenceResolver
import java.util.HashSet
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.utils.addIfNotNull

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
            val fqName = originalDescriptor.getFqName().asString()
            result.addIfNotNull(psiFacade.findPackage(fqName))
            result.addIfNotNull(psiFacade.findClass(fqName, project.allScope()))
        }
        // filter out elements which are navigate to some other element of the result
        // this is needed to avoid duplicated results for references to declaration in same library source file
        return result.filter { element -> result.none { element != it && it.getNavigationElement() == element } }
    }

    private fun findDecompiledAndBuiltInDeclarations(project: Project, descriptor: DeclarationDescriptor): Collection<PsiElement> {
        val libraryReferenceResolver = project.getComponent(javaClass<BuiltInsReferenceResolver>())
        val elements = libraryReferenceResolver!!.resolveBuiltInSymbol(descriptor)
        if (elements.isNotEmpty()) {
            return elements
        }

        val decompiledDeclaration = DecompiledNavigationUtils.getDeclarationFromDecompiledClassFile(project, descriptor)
        if (decompiledDeclaration != null) {
            return setOf(decompiledDeclaration)
        }
        return setOf()
    }
}
