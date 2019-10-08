/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.javac.wrappers.symbols

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.PackageElement

// Useful only for packages with JvmPackageName annotation
class MappedSymbolBasedPackage(
    private val originalFqName: FqName,
    private val childrenPackages: List<SimpleSymbolBasedPackage>,
    javac: JavacWrapper
) : SymbolBasedElement<PackageElement>(childrenPackages.first().element, javac), SymbolBasedPackage {
    override val fqName: FqName
        get() = originalFqName

    override val subPackages: Collection<JavaPackage>
        get() = childrenPackages.flatMap { javac.findSubPackages(it.fqName) }


    override val annotations: Collection<JavaAnnotation>
        get() = childrenPackages.map { childrenPackage ->
            childrenPackage.element.annotationMirrors.map { annotationMirror -> SymbolBasedAnnotation(annotationMirror, javac) }
        }.flatten()

    override val annotationsByFqName: Map<FqName?, JavaAnnotation> by buildLazyValueForMap()

    // @JvmPackageName-annotated files cannot have classes
    override fun getClasses(nameFilter: (Name) -> Boolean): List<JavaClass> = emptyList()

    override fun toString() = originalFqName.toString()

}