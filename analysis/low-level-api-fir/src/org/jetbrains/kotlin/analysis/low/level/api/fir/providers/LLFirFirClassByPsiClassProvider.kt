/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.compiled.ClsElementImpl
import org.jetbrains.kotlin.analysis.api.utils.errors.withClassEntry
import org.jetbrains.kotlin.analysis.api.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.analysis.utils.classId
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.java.FirJavaAwareSymbolProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

class LLFirFirClassByPsiClassProvider(private val session: LLFirSession) : FirSessionComponent {
    private val conflictsPsiClassToFirClassCache =
        session.firCachesFactory.createCache<PsiClass, FirRegularClassSymbol, ConflictsPsiClassToFirClassCacheContext> { psiClass, (javaFacade, parent) ->
            val classId = psiClass.classIdOrThrowError()
            val symbol = FirRegularClassSymbol(classId)
            val javaClass = JavaClassImpl(psiClass)

            javaFacade.convertJavaClassToFir(symbol, parent, javaClass).symbol
        }

    private data class ConflictsPsiClassToFirClassCacheContext(
        val javaFacade: FirJavaFacade,
        val parentClass: FirRegularClassSymbol?,
    )

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
            candidateAttachment("Requested", psiClass)
            candidateAttachment("Got", gotPsi)
        }
        return firClassSymbol
    }

    private fun ExceptionAttachmentBuilder.candidateAttachment(name: String, canidate: PsiElement?) {
        withEntryGroup(name) {
            withClassEntry("psiElementClass", canidate)
            withEntry("path", canidate?.containingFile?.virtualFile?.path)
            withEntry("modificationStamp", canidate?.containingFile?.modificationStamp?.toString())
        }
    }

    private fun createFirClassFromFirProvider(psiClass: PsiClass): FirRegularClassSymbol {
        val javaAwareSymbolProvider = getFirJavaAwareSymbolProvider()
        return javaAwareSymbolProvider.getClassSymbolByPsiClass(psiClass)
    }

    private fun getFirJavaAwareSymbolProvider(): FirJavaAwareSymbolProvider {
        session.nullableJavaSymbolProvider?.let { return it }
        return session.symbolProvider.providers.firstIsInstance<FirJavaAwareSymbolProvider>()
    }

    internal fun FirJavaAwareSymbolProvider.getClassSymbolByPsiClass(psiClass: PsiClass): FirRegularClassSymbol {
        require(this is FirSymbolProvider)
        val classId = psiClass.classIdOrThrowError()
        val result = getClassLikeSymbolByClassId(classId)

        if (result?.fir?.psi == psiClass) {
            // found in the classpath as a first entry
            return result as FirRegularClassSymbol
        }

        val parentClass = psiClass.containingClass?.let { getClassSymbolByPsiClass(it) }
        return conflictsPsiClassToFirClassCache.getValue(psiClass, ConflictsPsiClassToFirClassCacheContext(javaFacade, parentClass))
    }

    private fun PsiClass.classIdOrThrowError(): ClassId =
        classId
            ?: errorWithAttachment("No classId for non-local class") {
                withPsiEntry("psiClass", this@classIdOrThrowError, session.ktModule)
                withEntry("qualifiedName", qualifiedName)
            }
}

internal val FirSession.nullableJavaSymbolProvider: JavaSymbolProvider? by FirSession.nullableSessionComponentAccessor()

val LLFirSession.firClassByPsiClassProvider: LLFirFirClassByPsiClassProvider by FirSession.sessionComponentAccessor()
