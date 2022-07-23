/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.utils.errors.withPsiAttachment
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.errorWithAttachment

class LLFirFirClassByPsiClassProvider(private val session: LLFirSession) : FirSessionComponent {
    fun getFirClass(psiClass: PsiClass): FirRegularClassSymbol? {
        require(psiClass !is PsiTypeParameter) {
            "${LLFirFirClassByPsiClassProvider::class.simpleName} can create only regular classes"
        }

        require(psiClass !is KtLightClassMarker) {
            "${LLFirFirClassByPsiClassProvider::class.simpleName} can create only non-kotlin classes"
        }

        if (psiClass.qualifiedName == null) {
            return null // not yet supported
        }

        val firClassSymbol = createFirClassFromFirProvider(psiClass)
        check(firClassSymbol.fir.psi == psiClass)
        return firClassSymbol
    }

    private fun createFirClassFromFirProvider(psiClass: PsiClass): FirRegularClassSymbol {
        val classId = psiClass.classIdIfNonLocal
            ?: error("No classId for non-local class")
        val provider = session.nullableJavaSymbolProvider ?: session.symbolProvider
        val symbol = provider.getClassLikeSymbolByClassId(classId)
            ?: errorWithAttachment("No classifier found") {
                withPsiAttachment("psiClass", psiClass)
                withAttachment("classId", classId.asString())
            }
        return symbol as FirRegularClassSymbol
    }
}

private val FirSession.nullableJavaSymbolProvider: JavaSymbolProvider? by FirSession.nullableSessionComponentAccessor()


val LLFirSession.firClassByPsiClassProvider: LLFirFirClassByPsiClassProvider by FirSession.sessionComponentAccessor()

private val PsiClass.classIdIfNonLocal: ClassId?
    get() {
        val packageName = (containingFile as? PsiJavaFile)?.packageName ?: return null
        val packageFqName = FqName(packageName)

        val classesNames = generateSequence(this) { it.containingClass }.map { it.name }.toList().asReversed()
        if (classesNames.any { it == null }) return null
        return ClassId(packageFqName, FqName(classesNames.joinToString(separator = ".")), false)
    }