/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

sealed class HLApplicator<in PSI : PsiElement, in INPUT : HLApplicatorInput> {
    abstract fun applyTo(psi: PSI, input: INPUT, project: Project?, editor: Editor?)

    abstract fun isApplicableByPsi(psi: PSI): Boolean

    abstract fun getActionName(psi: PSI, input: INPUT): String
    abstract fun getFamilyName(): String
}

fun <PSI : PsiElement, NEW_PSI : PSI, INPUT : HLApplicatorInput> HLApplicator<PSI, INPUT>.with(
    init: HLApplicatorBuilder<NEW_PSI, INPUT>.(olApplicator: HLApplicator<PSI, INPUT>) -> Unit
): HLApplicator<NEW_PSI, INPUT> = when (this@with) {
    is HLApplicatorImpl -> {
        val builder = HLApplicatorBuilder(applyTo, isApplicableByPsi, getActionName, getFamilyName)
        @Suppress("UNCHECKED_CAST")
        init(builder as HLApplicatorBuilder<NEW_PSI, INPUT>, this)
        builder.build()
    }
}

fun <PSI : PsiElement, NEW_PSI : PSI, INPUT : HLApplicatorInput> HLApplicator<PSI, INPUT>.with(
    newPsiTypeTag: KClass<NEW_PSI>,
    init: HLApplicatorBuilder<NEW_PSI, INPUT>.(olApplicator: HLApplicator<PSI, INPUT>) -> Unit
): HLApplicator<NEW_PSI, INPUT> = when (this@with) {
    is HLApplicatorImpl -> {
        val builder = HLApplicatorBuilder(applyTo, isApplicableByPsi, getActionName, getFamilyName)
        @Suppress("UNCHECKED_CAST")
        init(builder as HLApplicatorBuilder<NEW_PSI, INPUT>, this)
        builder.build()
    }
}


internal class HLApplicatorImpl<PSI : PsiElement, INPUT : HLApplicatorInput>(
    val applyTo: (PSI, INPUT, Project?, Editor?) -> Unit,
    val isApplicableByPsi: (PSI) -> Boolean,
    val getActionName: (PSI, INPUT) -> String,
    val getFamilyName: () -> String,
) : HLApplicator<PSI, INPUT>() {
    override fun applyTo(psi: PSI, input: INPUT, project: Project?, editor: Editor?) {
        applyTo.invoke(psi, input, project, editor)
    }

    override fun isApplicableByPsi(psi: PSI): Boolean =
        isApplicableByPsi.invoke(psi)

    override fun getActionName(psi: PSI, input: INPUT): String =
        getActionName.invoke(psi, input)

    override fun getFamilyName(): String =
        getFamilyName.invoke()
}


class HLApplicatorBuilder<PSI : PsiElement, INPUT : HLApplicatorInput> internal constructor(
    @PrivateForInline
    var applyTo: ((PSI, INPUT, Project?, Editor?) -> Unit)? = null,
    private var isApplicableByPsi: ((PSI) -> Boolean)? = null,
    private var getActionName: ((PSI, INPUT) -> String)? = null,
    private var getFamilyName: (() -> String)? = null
) {
    fun familyName(name: String) {
        getFamilyName = { name }
    }

    fun familyName(getName: () -> String) {
        getFamilyName = getName
    }

    fun familyAndActionName(getName: () -> String) {
        getFamilyName = getName
        getActionName = { _, _ -> getName() }
    }

    fun actionName(getActionName: (PSI, INPUT) -> String) {
        this.getActionName = getActionName
    }

    @OptIn(PrivateForInline::class)
    fun applyTo(doApply: (PSI, INPUT, Project?, Editor?) -> Unit) {
        applyTo = doApply
    }


    @OptIn(PrivateForInline::class)
    fun applyTo(doApply: (PSI, INPUT) -> Unit) {
        applyTo = { element, data, _, _ -> doApply(element, data) }
    }

    @OptIn(PrivateForInline::class)
    fun applyTo(doApply: (PSI, INPUT, Project?) -> Unit) {
        applyTo = { element, data, project, _ -> doApply(element, data, project) }
    }

    @OptIn(ExperimentalTypeInference::class)
    fun isApplicableByPsi(isApplicable: ((PSI) -> Boolean)? = null) {
        this.isApplicableByPsi = isApplicable
    }


    @OptIn(PrivateForInline::class)
    fun build(): HLApplicator<PSI, INPUT> = HLApplicatorImpl(
        applyTo = applyTo!!,
        isApplicableByPsi = isApplicableByPsi ?: { true },
        getActionName = getActionName ?: getFamilyName?.let { familyName -> { _, _ -> familyName.invoke() } }!!,
        getFamilyName = getFamilyName!!
    )
}


fun <PSI : PsiElement, INPUT : HLApplicatorInput> applicator(
    init: HLApplicatorBuilder<PSI, INPUT>.() -> Unit,
): HLApplicator<PSI, INPUT> =
    HLApplicatorBuilder<PSI, INPUT>().apply(init).build()
