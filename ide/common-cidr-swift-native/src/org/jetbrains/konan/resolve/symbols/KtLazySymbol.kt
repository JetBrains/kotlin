package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbolBase
import org.jetbrains.konan.resolve.symbols.KtLazySymbol.StubState
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

abstract class KtLazySymbol<State : StubState, Stb : ObjCTopLevel<*>> : KtSymbol {
    private lateinit var name: String

    @Volatile
    private var _state: StubState? = null

    @Suppress("UNCHECKED_CAST")
    private val validStateOrNull: State?
        get() = _state.takeUnless { it is AbortedState } as State?

    // TODO: Remove superfluous ModuleDescriptor reference, when Kotlin/Native 1.4.0-M2 lands
    @Transient
    @Volatile
    private var stubAndProject: Triple<ModuleDescriptor, Stb, Project>?

    constructor(
        moduleDescriptor: ModuleDescriptor,
        stub: Stb,
        project: Project,
        name: String
    ) {
        this.name = name
        this.stubAndProject = Triple(moduleDescriptor, stub, project)
    }

    constructor() {
        this.stubAndProject = null
    }

    protected val state: State?
        get() {
            _state?.let { return validStateOrNull }
            stubAndProject?.let { (moduleDescriptor, stub, project) ->
                val newState = runReadAction {
                    if (project.isDisposed || !moduleDescriptor.isValid) {
                        return@runReadAction null
                    }
                    computeState(stub, project)
                }
                if (valueUpdater.compareAndSet(this, null, newState ?: AbortedState(stub))) {
                    stubAndProject = null
                    return newState
                }
            }
            return validStateOrNull
        }

    private fun psi(project: Project): PsiElement? {
        stubAndProject?.let { (_, stub, _) -> return stub.psi }
        return OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)
    }

    override fun getName(): String = name

    override fun getComplexOffset(): Long =
        _state?.offset
            ?: stubAndProject?.let { (_, stub, _) -> stub.offset }
            ?: _state!!.offset

    protected abstract fun computeState(stub: Stb, project: Project): State

    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean {
        val f = first as KtLazySymbol<*, *>
        val s = second as KtLazySymbol<*, *>

        if (!Comparing.equal(f.state, s.state)) return false

        return true
    }

    override fun locateDefinition(project: Project): PsiElement? = psi(project)?.let { KtOCSymbolPsiWrapper(it, this) }

    override fun updateOffset(start: Int, lengthShift: Int) {
        if (validStateOrNull == null) return
        super.updateOffset(start, lengthShift)
    }

    override fun setComplexOffset(complexOffset: Long) {
        _state!!.offset = complexOffset
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

    internal class AbortedState : StubState {
        constructor(stub: Stub<*>) : super(stub)
        constructor() : super()
    }

    companion object {
        private val valueUpdater: AtomicReferenceFieldUpdater<KtLazySymbol<*, *>, StubState> = AtomicReferenceFieldUpdater.newUpdater(
            KtLazySymbol::class.java,
            StubState::class.java,
            "_state"
        )
    }
}
