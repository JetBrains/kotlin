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
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.decompiler.navigation.DecompiledNavigationUtils
import org.jetbrains.kotlin.idea.references.BuiltInsReferenceResolver
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import java.util.*
import org.jetbrains.kotlin.descriptors.*

public object DescriptorToSourceUtilsIde {
    public fun getAnyDeclaration(project: Project, descriptor: DeclarationDescriptor): PsiElement? {
        return getDeclarationsStream(project, descriptor).firstOrNull()
    }

    public fun getAllDeclarations(project: Project, targetDescriptor: DeclarationDescriptor): Collection<PsiElement> {
        val result = getDeclarationsStream(project, targetDescriptor).toHashSet()
        // filter out elements which are navigate to some other element of the result
        // this is needed to avoid duplicated results for references to declaration in same library source file
        return result.filter { element -> result.none { element != it && it.getNavigationElement() == element } }
    }

    private fun getDeclarationsStream(project: Project, targetDescriptor: DeclarationDescriptor): Stream<PsiElement> {
        val effectiveReferencedDescriptors = DescriptorToSourceUtils.getEffectiveReferencedDescriptors(targetDescriptor).stream()
        return effectiveReferencedDescriptors.flatMap {
            streamOf(
                    DescriptorToSourceUtils.getSourceFromDescriptor(it),
                    findBuiltinDeclaration(project, it),
                    DecompiledNavigationUtils.getDeclarationFromDecompiledClassFile(project, it)
            )
        }.filterNotNull()
    }

    private fun findBuiltinDeclaration(project: Project, descriptor: DeclarationDescriptor): PsiElement? {
        val libraryReferenceResolver = project.getComponent(javaClass<BuiltInsReferenceResolver>())
        return libraryReferenceResolver!!.resolveBuiltInSymbol(descriptor)
    }
}
