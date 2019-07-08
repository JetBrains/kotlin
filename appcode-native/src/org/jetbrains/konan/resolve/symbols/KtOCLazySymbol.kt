/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater

abstract class KtOCLazySymbol<State : KtOCLazySymbol.StubState, Stb : Stub<*>>
    : OCSymbol, OCForeignSymbol, VirtualFileOwner, KtSymbol {

    @Transient
    private lateinit var file: VirtualFile
    private lateinit var name: String

    @Volatile
    private var _state: State? = null

    @Transient
    @Volatile
    private var stubAndProject: Pair<Stb, Project>?

    constructor(
        stub: Stb,
        project: Project,
        file: VirtualFile
    ) {
        this.file = file
        this.name = stub.name
        this.stubAndProject = Pair(stub, project)
    }

    constructor() {
        this.stubAndProject = null
    }

    protected val state: State
        get() {
            _state?.let { return it }
            stubAndProject?.let { (stub, project) ->
                //todo check project.isDisposed
                val newState = runReadAction { computeState(stub, project) }
                if (valueUpdater.compareAndSet(this, null, newState)) {
                    stubAndProject = null
                    return newState
                }
            }
            return _state!!
        }

    private fun psi(project: Project): PsiElement? {
        stubAndProject?.let { return it.first.psi }
        return OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)
    }

    override fun getName(): String = name

    override fun getComplexOffset(): Long =
        _state?.offset
        ?: stubAndProject?.first?.offset
        ?: _state!!.offset

    protected abstract fun computeState(stub: Stb, project: Project): State

    override fun getContainingFile(): VirtualFile = file

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KtOCLazySymbol<*, *>
        val s = second as KtOCLazySymbol<*, *>

        if (!Comparing.equal(f.file, s.file)) return false
        if (!Comparing.equal(f.state, s.state)) return false

        return true
    }

    override fun hashCodeExcludingOffset(): Int = name.hashCode() * 31 + file.hashCode()

    override fun locateDefinition(project: Project): PsiElement? = psi(project)?.let { KtOCPsiWrapper(it, this) }

    override fun isSameSymbol(symbol: OCSymbol?, project: Project): Boolean {
        return super.isSameSymbol(symbol, project)
               || symbol is KtOCLightSymbol && psi(project) == symbol.locateDefinition(project)
    }

    override fun updateOffset(start: Int, lengthShift: Int) {
        if (_state == null) return
        super.updateOffset(start, lengthShift)
    }

    override fun setComplexOffset(complexOffset: Long) {
        _state!!.offset = complexOffset
    }

    override fun init(file: VirtualFile) {
        this.file = file
    }

    fun ensureStateLoaded() {
        state
    }

    abstract class StubState {
        constructor(stub: Stub<*>) {
            this.offset = stub.offset
        }

        constructor() {
            this.offset = 0
        }

        var offset: Long
            internal set
    }

    companion object {
        private val valueUpdater: AtomicReferenceFieldUpdater<KtOCLazySymbol<*, *>, StubState> = newUpdater(
            KtOCLazySymbol::class.java,
            KtOCLazySymbol.StubState::class.java,
            "_state"
        )
    }
}
