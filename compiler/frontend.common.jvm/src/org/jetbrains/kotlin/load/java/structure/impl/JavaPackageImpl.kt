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

package org.jetbrains.kotlin.load.java.structure.impl

import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaPackageImpl(
    psiPackageSource: JavaElementPsiSource<PsiPackage>, private val scope: GlobalSearchScope,
    private val mayHaveAnnotations: Boolean = true,
) : JavaElementImpl<PsiPackage>(psiPackageSource), JavaPackage, MapBasedJavaAnnotationOwner {

    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> {
        val psiClasses = psi.getClasses(scope).filter {
            val name = it.name
            name != null && nameFilter(Name.identifier(name))
        }
        return classes(psiClasses, sourceFactory)
    }

    override val subPackages: Collection<JavaPackage>
        get() = packages(psi.getSubPackages(scope), scope, sourceFactory)

    override val fqName: FqName
        get() = FqName(psi.qualifiedName)

    override val annotations: Collection<JavaAnnotation>
        get() =
            if (mayHaveAnnotations)
                org.jetbrains.kotlin.load.java.structure.impl.annotations(psi.annotationList?.annotations.orEmpty(), sourceFactory)
            else
                emptyList()

    override val annotationsByFqName: Map<FqName?, JavaAnnotation> by buildLazyValueForMap()
}
