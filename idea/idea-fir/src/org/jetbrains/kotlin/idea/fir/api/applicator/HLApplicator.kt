/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.applicator

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.idea.frontend.api.ForbidKtResolve
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

/**
 * Applies a fix to the PSI, used as intention/inspection/quickfix action
 * Also, knows if a fix is applicable by [isApplicableByPsi]
 *
 * Uses some additional information from [INPUT] to apply the element
 */
sealed class HLApplicator<in PSI : PsiElement, in INPUT : HLApplicatorInput> {

    /**
     * Applies some fix to given [psi], can not use resolve, so all needed data should be precalculated and stored in [input]
     *
     * @param psi a [PsiElement] to apply fix to
     * @param input additional data needed to apply the fix, the [input] can be collected by [HLApplicatorInputProvider
     */
    fun applyTo(psi: PSI, input: INPUT, project: Project?, editor: Editor?) = ForbidKtResolve.forbidResolveIn("HLApplicator.applyTo") {
        applyToImpl(psi, input, project, editor)
    }

    /**
     * Checks if applicator is applicable to specific element, can not use resolve inside
     */
    fun isApplicableByPsi(psi: PSI): Boolean = ForbidKtResolve.forbidResolveIn("HLApplicator.isApplicableByPsi") {
        isApplicableByPsiImpl(psi)
    }

    /**
     * Action name which will be as text in inspections/intentions
     *
     * @see com.intellij.codeInsight.intention.IntentionAction.getText
     */
    fun getActionName(psi: PSI, input: INPUT): String = ForbidKtResolve.forbidResolveIn("HLApplicator.getActionName") {
        getActionNameImpl(psi, input)
    }

    /**
     * Family name which will be used in inspections/intentions
     *
     * @see com.intellij.codeInsight.intention.IntentionAction.getFamilyName
     */
    fun getFamilyName(): String = ForbidKtResolve.forbidResolveIn("HLApplicator.getFamilyName") {
        getFamilyNameImpl()
    }

    protected abstract fun applyToImpl(psi: PSI, input: INPUT, project: Project?, editor: Editor?)
    protected abstract fun isApplicableByPsiImpl(psi: PSI): Boolean
    protected abstract fun getActionNameImpl(psi: PSI, input: INPUT): String
    protected abstract fun getFamilyNameImpl(): String
}

/**
 * Create a copy of an applicator with some components replaced
 */
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

/**
 * Create a copy of an applicator with some components replaced
 * The PSI type of a new applicator will be a class passed in [newPsiTypeTag]
 */
fun <PSI : PsiElement, NEW_PSI : PSI, INPUT : HLApplicatorInput> HLApplicator<PSI, INPUT>.with(
    @Suppress("UNUSED_PARAMETER") newPsiTypeTag: KClass<NEW_PSI>,
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
    override fun applyToImpl(psi: PSI, input: INPUT, project: Project?, editor: Editor?) {
        applyTo.invoke(psi, input, project, editor)
    }

    override fun isApplicableByPsiImpl(psi: PSI): Boolean =
        isApplicableByPsi.invoke(psi)

    override fun getActionNameImpl(psi: PSI, input: INPUT): String =
        getActionName.invoke(psi, input)

    override fun getFamilyNameImpl(): String =
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
    fun build(): HLApplicator<PSI, INPUT> {
        val applyTo = applyTo
            ?: error("Please, specify applyTo")
        val getActionName = getActionName
            ?: error("Please, specify actionName or familyName via either of: actionName,familyAndActionName")
        val isApplicableByPsi = isApplicableByPsi ?: { true }
        val getFamilyName = getFamilyName
            ?: error("Please, specify or familyName via either of: familyName, familyAndActionName")
        return HLApplicatorImpl(
            applyTo = applyTo,
            isApplicableByPsi = isApplicableByPsi,
            getActionName = getActionName,
            getFamilyName = getFamilyName
        )
    }
}


/**
 * Builds a new applicator with HLApplicatorBuilder
 *
 *  Should specify at least applyTo and familyAndActionName
 *
 *  @see HLApplicatorBuilder
 */
fun <PSI : PsiElement, INPUT : HLApplicatorInput> applicator(
    init: HLApplicatorBuilder<PSI, INPUT>.() -> Unit,
): HLApplicator<PSI, INPUT> =
    HLApplicatorBuilder<PSI, INPUT>().apply(init).build()
