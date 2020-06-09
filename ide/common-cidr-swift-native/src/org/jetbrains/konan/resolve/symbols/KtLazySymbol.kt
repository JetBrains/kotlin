package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.CidrLog
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbolBase
import org.jetbrains.konan.resolve.symbols.KtLazySymbol.StubState
import org.jetbrains.konan.resolve.translation.TranslationState
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.Stub
import org.jetbrains.kotlin.descriptors.InvalidModuleException
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
    private var translationState: TranslationState<Stb>? = null

    constructor(translationState: TranslationState<Stb>, name: String) {
        this.name = name
        this.translationState = translationState
    }

    constructor()

    protected val state: State?
        get() {
            _state?.let { return validStateOrNull }
            translationState?.run {
                val newState = runReadAction {
                    if (!stubTrace.isValid) return@runReadAction null
                    try {
                        computeState(stub, project)
                    } catch (e: AssertionError) {
                        CidrLog.LOG.error(e) // likely ObjC backend failed to generate stubs
                        null
                    } catch (e: InvalidModuleException) {
                        CidrLog.LOG.error(e)
                        null
                    }
                }
                if (valueUpdater.compareAndSet(this@KtLazySymbol, null, newState ?: AbortedState(stub))) {
                    if (newState != null) didCompleteSuccessfully()
                    clearTranslationState()
                    return newState
                }
            }
            return validStateOrNull
        }

    private fun psi(project: Project): PsiElement? {
        translationState?.let { (_, stub) -> return stub.psi }
        return OCSymbolBase.doLocateDefinition(this, project, KtNamedDeclaration::class.java)
    }

    override fun getName(): String = name

    override fun getComplexOffset(): Long =
        _state?.offset
            ?: translationState?.let { (_, stub) -> stub.offset }
            ?: _state!!.offset

    protected abstract fun computeState(stub: Stb, project: Project): State

    protected open fun clearTranslationState() {
        translationState = null
    }

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

    val stateLoaded: Boolean
        get() = validStateOrNull != null

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

        fun useLazyTranslation(): Boolean = Registry.`is`("konan.enable.lazy.symbol.translation", false)
    }
}
