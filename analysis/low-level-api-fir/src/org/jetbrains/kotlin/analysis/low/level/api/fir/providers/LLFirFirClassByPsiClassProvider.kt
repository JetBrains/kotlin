/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.compiled.ClsElementImpl
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.utils.errors.withPsiAttachment
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.checkWithAttachment
import org.jetbrains.kotlin.utils.errorWithAttachment
import org.jetbrains.kotlin.utils.withAttachmentBuilder

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
            it.withAttachmentBuilder("psiClass") {
                appendLine("file: ${psiClass.containingFile?.virtualFile?.path}")
                appendLine("PsiClass:")
                appendLine(psiClass.text)
            }
        }

        if (psiClass.qualifiedName == null) {
            return null // not yet supported
        }

        val firClassSymbol = createFirClassFromFirProvider(psiClass)
        checkWithAttachment(
            firClassSymbol.fir.psi == psiClass,
            { "resulted FirClass.psi != requested PsiClass" }
        ) {
            it.withAttachmentBuilder("info") {
                appendLine("Requested ${psiClass::class.java.name}:")
                appendLine("path: ${psiClass.containingFile?.virtualFile?.path}")
                appendLine("modificationStamp: ${psiClass.containingFile.modificationStamp}")
                appendLine()
                appendLine("Got ${firClassSymbol.fir.psi!!::class.java.name}")
                appendLine("path: ${firClassSymbol.fir.psi?.containingFile?.virtualFile?.path}")
                appendLine("modificationStamp: ${firClassSymbol.fir.psi?.containingFile?.modificationStamp}")
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
        val packageName = (containingFile as? PsiClassOwner)?.packageName ?: return null
        val qualifiedName = qualifiedName ?: return null
        val relatedClassName = qualifiedName.removePrefix("$packageName.")
        if (relatedClassName.isEmpty()) return null

        return ClassId(FqName(packageName), FqName(relatedClassName), false)
    }