/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

interface FirModuleResolveState {
    val sessionProvider: FirProjectSessionProvider

    fun getSession(psi: KtElement): FirSession {
        val moduleInfo = psi.getModuleInfo() as ModuleSourceInfo
        return getSession(psi.project, moduleInfo)
    }

    fun getSession(project: Project, moduleInfo: ModuleSourceInfo): FirSession {
        sessionProvider.getSession(moduleInfo)?.let { return it }
        return synchronized(moduleInfo.module) {
            val session = sessionProvider.getSession(moduleInfo) ?: FirIdeJavaModuleBasedSession.create(
                project, moduleInfo, sessionProvider, moduleInfo.contentScope()
            ).also { moduleBasedSession ->
                sessionProvider.sessionCache[moduleInfo] = moduleBasedSession
            }
            session.also {
                it.extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
            }
        }
    }

    operator fun get(psi: KtElement): FirElement?

    fun getDiagnostics(psi: KtElement): List<Diagnostic>

    fun hasDiagnosticsForFile(file: KtFile): Boolean

    fun record(psi: KtElement, fir: FirElement)

    fun record(psi: KtElement, diagnostic: Diagnostic)

    fun setDiagnosticsForFile(file: KtFile, fir: FirFile, diagnostics: Iterable<FirDiagnostic<*>> = emptyList())
}

class FirModuleResolveStateImpl(override val sessionProvider: FirProjectSessionProvider) : FirModuleResolveState {
    private val cache = mutableMapOf<KtElement, FirElement>()

    private val diagnosticCache = mutableMapOf<KtElement, MutableList<Diagnostic>>()

    private val diagnosedFiles = mutableMapOf<KtFile, Long>()

    override fun get(psi: KtElement): FirElement? = cache[psi]

    override fun getDiagnostics(psi: KtElement): List<Diagnostic> {
        return diagnosticCache[psi] ?: emptyList()
    }

    override fun hasDiagnosticsForFile(file: KtFile): Boolean {
        val previousStamp = diagnosedFiles[file] ?: return false
        if (file.modificationStamp == previousStamp) {
            return true
        }
        diagnosedFiles.remove(file)
        file.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                cache.remove(element)
                diagnosticCache.remove(element)
                element.acceptChildren(this)
                super.visitElement(element)
            }
        })
        return false
    }

    override fun record(psi: KtElement, fir: FirElement) {
        cache[psi] = fir
    }

    override fun record(psi: KtElement, diagnostic: Diagnostic) {
        val list = diagnosticCache.getOrPut(psi) { mutableListOf() }
        list += diagnostic
    }

    override fun setDiagnosticsForFile(file: KtFile, fir: FirFile, diagnostics: Iterable<FirDiagnostic<*>>) {
        for (diagnostic in diagnostics) {
            require(diagnostic is FirPsiDiagnostic<*>)
            val psi = diagnostic.element.psi as? KtElement ?: continue
            record(psi, diagnostic.asPsiBasedDiagnostic())
        }

        diagnosedFiles[file] = file.modificationStamp
    }
}

fun KtElement.firResolveState(): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project).getResolveState(getModuleInfo())