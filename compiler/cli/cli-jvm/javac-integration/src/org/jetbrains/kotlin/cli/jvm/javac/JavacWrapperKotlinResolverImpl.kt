/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.javac

import org.jetbrains.kotlin.javac.JavacWrapperKotlinResolver
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

class JavacWrapperKotlinResolverImpl : JavacWrapperKotlinResolver {
    override fun resolveSupertypes(classOrObject: KtClassOrObject): List<ClassId> = emptyList()
    override fun findField(classOrObject: KtClassOrObject, name: String): JavaField? = null
    override fun findField(ktFile: KtFile?, name: String): JavaField? = null
}
