/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.*
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cfg.containingDeclarationForPseudocode
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import kotlin.reflect.KProperty


internal inline fun <T> executeOrReturnDefaultValueOnPCE(defaultValue: T, action: () -> T): T =
    try {
        action()
    } catch (e: ProcessCanceledException) {
        defaultValue
    }

internal inline fun <T> executeWithoutPCE(crossinline action: () -> T): T {
    var result: T? = null
    ProgressManager.getInstance().executeNonCancelableSection { result = action() }
    @Suppress("UNCHECKED_CAST")
    return result as T
}

internal inline fun <T> Lock.lockWithPCECheck(lockingIntervalMs: Long, action: () -> T): T {
    var needToRun = true
    var result: T? = null
    while (needToRun) {
        checkCanceled()
        if (tryLock(lockingIntervalMs, TimeUnit.MILLISECONDS)) {
            try {
                needToRun = false
                result = action()
            } finally {
                unlock()
            }
        }
    }
    return result!!
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkCanceled() {
    ProgressManager.checkCanceled()
}

internal val FirElement.isErrorElement
    get() = this is FirDiagnosticHolder

internal val FirDeclaration.ktDeclaration: KtDeclaration
    get() {
        val psi = psi
            ?: error("PSI element was not found for${render()}")
        return when (psi) {
            is KtDeclaration -> psi
            is KtObjectLiteralExpression -> psi.objectDeclaration
            else -> error(
                """
                   FirDeclaration.psi (${this::class.simpleName}) should be KtDeclaration but was ${psi::class.simpleName}
                   ${(psi as? KtElement)?.getElementTextInContext() ?: psi.text}
                   
                   ${render()}
                   """.trimIndent()
            )
        }
    }

internal val FirDeclaration.containingKtFileIfAny: KtFile?
    get() = psi?.containingFile as? KtFile


internal fun ModuleInfo.collectTransitiveDependenciesWithSelf(): List<ModuleInfo> {
    val result = mutableSetOf<ModuleInfo>()
    fun collect(module: ModuleInfo) {
        if (module in result) return
        result += module
        module.dependencies().forEach(::collect)
    }
    collect(this)
    return result.toList()
}

internal fun KtDeclaration.isNonAnonymousClassOrObject() =
    this is KtClassOrObject
            && !this.isObjectLiteral()
            && this !is KtEnumEntry


@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> CachedValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

internal inline fun <T> cachedValue(project: Project, vararg dependencies: Any, crossinline createValue: () -> T) =
    CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(
            createValue(),
            dependencies
        )
    }

/**
 * Creates a value which will be cached until until any physical PSI change happens
 *
 * @see com.intellij.psi.util.CachedValue
 * @see com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
 */
internal fun <T> psiModificationTrackerBasedCachedValue(project: Project, createValue: () -> T) =
    cachedValue(project, PsiModificationTracker.MODIFICATION_COUNT, createValue = createValue)

fun KtElement.getElementTextInContext(): String {
    val context = parentOfType<KtImportDirective>()
        ?: parentOfType<KtPackageDirective>()
        ?: containingDeclarationForPseudocode
        ?: containingKtFile
    val builder = StringBuilder()
    context.accept(object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element === this@getElementTextInContext) builder.append("<$ELEMENT_TAG>")
            if (element is LeafPsiElement) {
                builder.append(element.text)
            } else {
                element.acceptChildren(this)
            }
            if (element === this@getElementTextInContext) builder.append("</$ELEMENT_TAG>")
        }
    })
    return builder.toString().trimIndent().trim()
}

private const val ELEMENT_TAG = "ELEMENT"

inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

fun <T : PsiElement> PsiElement.parentsOfType(clazz: Class<out T>, withSelf: Boolean = true): Sequence<T> {
    return (if (withSelf) parentsWithSelf else parents).filterIsInstance(clazz)
}

inline fun <reified T : PsiElement> PsiElement.parentsOfType(withSelf: Boolean = true): Sequence<T> =
    parentsOfType(T::class.java, withSelf)
