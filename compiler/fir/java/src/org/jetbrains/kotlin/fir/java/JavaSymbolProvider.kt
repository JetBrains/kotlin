/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.java.symbols.JavaClassSymbol
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

class JavaSymbolProvider(
    val project: Project,
    private val searchScope: GlobalSearchScope
) : AbstractFirSymbolProvider() {

    override fun getSymbolByFqName(classId: ClassId): ConeSymbol? {
        return classCache.lookupCacheOrCalculate(classId) {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val foundClass = facade.findClass(JavaClassFinder.Request(classId), searchScope)
            foundClass?.let { javaClass -> JavaClassSymbol(javaClass) }
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.lookupCacheOrCalculate(fqName) {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val javaPackage = facade.findPackage(fqName.asString(), searchScope) ?: return@lookupCacheOrCalculate null
            FqName(javaPackage.qualifiedName)
        }
    }
}

