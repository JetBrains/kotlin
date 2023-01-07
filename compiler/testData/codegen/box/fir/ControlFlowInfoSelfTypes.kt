// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.Self

interface ReadOnlyControlFlowInfo<K : Any, D : Any> {
    fun getOrNull(key: K): D?

    // Only used in tests
    fun asMap(): Map<K, D>
}

@Self
abstract class ControlFlowInfo<K : Any, D : Any>
internal constructor(
    protected val map: Map<K, D> = mapOf()
) : Map<K, D> by map, ReadOnlyControlFlowInfo<K, D> {
    protected abstract fun copy(newMap: Map<K, D>): Self

    fun put(key: K, value: D): Self =
        put(key, value, this[key] ?: null as D?)

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the persistent hashmap again
     */
    fun put(key: K, value: D, oldValue: D?): Self {
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as Self
        val newMap = map + (key to value)
        return copy(newMap)
    }

    override fun getOrNull(key: K): D? = this[key] ?: null as D?
    override fun asMap() = this

    fun retainAll(predicate: (K) -> Boolean): Self {
        val newMap = map.filter { predicate(it.key) }
        return copy(newMap)
    }

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()
}

// ALIASES BEGIN

typealias VariableDescriptor = String
typealias VariableUsageControlFlowInfo<S, D> = ControlFlowInfo<VariableDescriptor, D, S>
typealias VariableUsageReadOnlyControlInfo = ReadOnlyControlFlowInfo<VariableDescriptor, VariableUseState>

// ALIASES END


// IMPLEMENTATIONS OF CONTROL FLOW INFOS BEGIN

class UsageVariableControlFlowInfo(map: Map<VariableDescriptor, VariableUseState> = mapOf()) :
    VariableUsageControlFlowInfo<UsageVariableControlFlowInfo, VariableUseState>(map),
    VariableUsageReadOnlyControlInfo {
    override fun copy(newMap: Map<VariableDescriptor, VariableUseState>): UsageVariableControlFlowInfo  =
        UsageVariableControlFlowInfo(newMap)
}

interface VariableInitReadOnlyControlFlowInfo :
    ReadOnlyControlFlowInfo<VariableDescriptor, VariableControlFlowState> {
    fun checkDefiniteInitializationInWhen(merge: VariableInitReadOnlyControlFlowInfo): Boolean
}

class VariableInitControlFlowInfo(map: Map<VariableDescriptor, VariableControlFlowState> = mapOf()) :
    VariableUsageControlFlowInfo<VariableInitControlFlowInfo, VariableControlFlowState>(map),
    VariableInitReadOnlyControlFlowInfo {
    override fun copy(newMap: Map<VariableDescriptor, VariableControlFlowState>): VariableInitControlFlowInfo =
        VariableInitControlFlowInfo(newMap)

    // this = output of EXHAUSTIVE_WHEN_ELSE instruction
    // merge = input of MergeInstruction
    // returns true if definite initialization in when happens here
    override fun checkDefiniteInitializationInWhen(merge: VariableInitReadOnlyControlFlowInfo): Boolean {
        for ((key, value) in iterator()) {
            if (value.initState == InitState.INITIALIZED_EXHAUSTIVELY &&
                merge.getOrNull(key)?.initState == InitState.INITIALIZED
            ) {
                return true
            }
        }
        return false
    }
}

// IMPLEMENTATIONS OF CONTROL FLOW INFOS END

// STATES BEGIN

class VariableControlFlowState private constructor(val initState: InitState, val isDeclared: Boolean) {

    fun definitelyInitialized(): Boolean = initState == InitState.INITIALIZED

    fun mayBeInitialized(): Boolean = initState != InitState.NOT_INITIALIZED

    override fun toString(): String {
        if (initState == InitState.NOT_INITIALIZED && !isDeclared) return "-"
        return "$initState${if (isDeclared) "D" else ""}"
    }

    companion object {

        private val VS_IT = VariableControlFlowState(InitState.INITIALIZED, true)
        private val VS_IF = VariableControlFlowState(InitState.INITIALIZED, false)
        private val VS_ET = VariableControlFlowState(InitState.INITIALIZED_EXHAUSTIVELY, true)
        private val VS_EF = VariableControlFlowState(InitState.INITIALIZED_EXHAUSTIVELY, false)
        private val VS_UT = VariableControlFlowState(InitState.UNKNOWN, true)
        private val VS_UF = VariableControlFlowState(InitState.UNKNOWN, false)
        private val VS_NT = VariableControlFlowState(InitState.NOT_INITIALIZED, true)
        private val VS_NF = VariableControlFlowState(InitState.NOT_INITIALIZED, false)

        fun create(initState: InitState, isDeclared: Boolean): VariableControlFlowState =
            when (initState) {
                InitState.INITIALIZED -> if (isDeclared) VS_IT else VS_IF
                InitState.INITIALIZED_EXHAUSTIVELY -> if (isDeclared) VS_ET else VS_EF
                InitState.UNKNOWN -> if (isDeclared) VS_UT else VS_UF
                InitState.NOT_INITIALIZED -> if (isDeclared) VS_NT else VS_NF
            }

        fun createInitializedExhaustively(isDeclared: Boolean): VariableControlFlowState =
            create(InitState.INITIALIZED_EXHAUSTIVELY, isDeclared)

        fun create(isInitialized: Boolean, isDeclared: Boolean = false): VariableControlFlowState =
            create(if (isInitialized) InitState.INITIALIZED else InitState.NOT_INITIALIZED, isDeclared)

        fun create(isDeclaredHere: Boolean, mergedEdgesData: VariableControlFlowState?): VariableControlFlowState =
            create(true, isDeclaredHere || mergedEdgesData != null && mergedEdgesData.isDeclared)
    }
}

enum class VariableUseState(private val priority: Int) {
    READ(3),
    WRITTEN_AFTER_READ(2),
    ONLY_WRITTEN_NEVER_READ(1),
    UNUSED(0);

    fun merge(variableUseState: VariableUseState?): VariableUseState {
        if (variableUseState == null || priority > variableUseState.priority) return this
        return variableUseState
    }

    companion object {

        @JvmStatic
        fun isUsed(variableUseState: VariableUseState?): Boolean =
            variableUseState != null && variableUseState != UNUSED
    }
}

enum class InitState(private val s: String) {
    // Definitely initialized
    INITIALIZED("I"),
    // Fake initializer in else branch of "exhaustive when without else", see MagicKind.EXHAUSTIVE_WHEN_ELSE
    INITIALIZED_EXHAUSTIVELY("IE"),
    // Initialized in some branches, not initialized in other branches
    UNKNOWN("I?"),
    // Definitely not initialized
    NOT_INITIALIZED("");

    fun merge(other: InitState): InitState {
        // X merge X = X
        // X merge IE = IE merge X = X
        // else X merge Y = I?
        if (this == other || other == INITIALIZED_EXHAUSTIVELY) return this
        if (this == INITIALIZED_EXHAUSTIVELY) return other
        return UNKNOWN
    }

    override fun toString() = s
}

// STATES END

fun box(): String {
    val usageVariableControlFlowInfo: UsageVariableControlFlowInfo = UsageVariableControlFlowInfo(
        mapOf(
            "unused" to VariableUseState.UNUSED,
            "read" to VariableUseState.READ
        )
    )

    val usageVariableControlFlowInfoUpdated: UsageVariableControlFlowInfo =
        usageVariableControlFlowInfo.put("second unused", VariableUseState.UNUSED)


    val variableInitControlFlowInfo: VariableInitControlFlowInfo = VariableInitControlFlowInfo(
        mapOf(
            "VS_IT" to VariableControlFlowState.create(InitState.INITIALIZED, isDeclared = true),
            "VS_IF" to VariableControlFlowState.create(InitState.INITIALIZED, isDeclared = false)
        )
    )

    val updatedVariableInitControlFlowInfo = variableInitControlFlowInfo.put(
        "VS_ET", VariableControlFlowState.create(InitState.INITIALIZED_EXHAUSTIVELY, isDeclared = true)
    )

    val predicate = usageVariableControlFlowInfoUpdated.containsKey("second unused") && updatedVariableInitControlFlowInfo.containsKey("VS_ET")

    return if (predicate) "OK" else "ERROR"
}
