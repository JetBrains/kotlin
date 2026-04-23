/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.model

import org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaPackageOverAst(
    override val fqName: FqName,
    private val finder: JavaClassFinderOverAstImpl,
) : JavaPackage {

    override val annotations: Collection<JavaAnnotation>
        get() = finder.getPackageAnnotations(fqName)

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName): JavaAnnotation? =
        annotations.find { it.classId?.asSingleFqName() == fqName }

    override val subPackages: Collection<JavaPackage>
        get() = finder.subPackagesOf(fqName).map { JavaPackageOverAst(it, finder) }

    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> =
        finder.classesInPackage(fqName, nameFilter)
}
