/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

abstract class LightClassGenerationSupport {

    abstract fun getContextForClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext

    abstract fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject>

    /*
     * Finds files whose package declaration is exactly {@code fqName}. For example, if a file declares
     *     package a.b.c
     * it will not be returned for fqName "a.b"
     *
     * If the resulting collection is empty, it means that this package has not other declarations than sub-packages
     */
    abstract fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile>

    // Returns only immediately declared classes/objects, package classes are not included (they have no declarations)
    abstract fun findClassOrObjectDeclarationsInPackage(
            packageFqName: FqName,
            searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject>

    abstract fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean

    abstract fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName>

    abstract fun getLightClass(classOrObject: KtClassOrObject): KtLightClass?

    abstract fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor?

    abstract fun analyze(element: KtElement): BindingContext

    abstract fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getMultifilePartClasses(partFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String>

    abstract fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtFile>

    abstract fun getContextForFacade(files: Collection<KtFile>): LightClassConstructionContext

    companion object {
        @JvmStatic fun getInstance(project: Project): LightClassGenerationSupport {
            return ServiceManager.getService(project, LightClassGenerationSupport::class.java)
        }
    }
}
