/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.compiled.ClsElementImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.utils.errors.*
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.analysis.utils.classIdIfNonLocal
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

class LLFirFirClassByPsiClassProvider(private val session: LLFirSession) : FirSessionComponent {
    fun getFirClass(psiClass: PsiClass): FirRegularClassSymbol? {
        require(psiClass !is PsiTypeParameter) {
            "${LLFirFirClassByPsiClassProvider::class.simpleName} can create only regular classes"
        }

        require(psiClass !is KtLightClassMarker) {
            "${LLFirFirClassByPsiClassProvider::class.simpleName} can create only non-kotlin classes"
        }

        checkWithAttachment(
            psiClass !is ClsElementImpl || !psiClass.hasAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString()), {
                "${LLFirFirClassByPsiClassProvider::class.simpleName} can create only non-kotlin classes, but got ${psiClass::class} with ${JvmAnnotationNames.METADATA_FQ_NAME.asString()} annotation"
            }
        ) {
            withEntry("virtualFilePath", psiClass.containingFile.virtualFile?.path)
            withPsiEntry("psiClass", psiClass, session.ktModule)
        }

        if (psiClass.qualifiedName == null) {
            return null // not yet supported
        }

        val firClassSymbol = createFirClassFromFirProvider(psiClass)
        val gotPsi = firClassSymbol.fir.psi
        checkWithAttachment(
            gotPsi == psiClass,
            { "resulted FirClass.psi != requested PsiClass" }
        ) {
            withEntryGroup("Requested") {
                withClassEntry("psiElementClass", psiClass)
                withEntry("path", psiClass.containingFile?.virtualFile?.path)
                withEntry("modificationStamp", psiClass.containingFile.modificationStamp.toString())
            }

            withEntryGroup("Got") {
                withClassEntry("psiElementClass", gotPsi)
                withEntry("path", gotPsi?.containingFile?.virtualFile?.path)
                withEntry("modificationStamp", gotPsi?.containingFile?.modificationStamp.toString())
            }
        }
        return firClassSymbol
    }

    private fun createFirClassFromFirProvider(psiClass: PsiClass): FirRegularClassSymbol {
        val classId = psiClass.classIdIfNonLocal
            ?: error("No classId for non-local class")
        val provider = session.nullableJavaSymbolProvider ?: session.symbolProvider
        val symbol = provider.getClassLikeSymbolByClassId(classId)
            ?: errorWithAttachment("No classifier found") {
                withPsiEntry("psiClass", psiClass, session.ktModule)
                withEntry("classId", classId) { it.asString() }
            }
        return symbol as FirRegularClassSymbol
    }
}

internal val FirSession.nullableJavaSymbolProvider: JavaSymbolProvider? by FirSession.nullableSessionComponentAccessor()

val LLFirSession.firClassByPsiClassProvider: LLFirFirClassByPsiClassProvider by FirSession.sessionComponentAccessor()
