// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-84904

// MODULE: lib
// FILE: lib.kt

val initializationLog = mutableListOf<String>()

abstract value class AbstractPairBase(first: Int, second: Int) {
    init {
        initializationLog += "base($first,$second)"
    }

    abstract fun score(): Int
}

// MODULE: main(lib)
// FILE: main.kt

value class PairScore(val first: Int, val second: Int) : AbstractPairBase(first, second) {
    init {
        initializationLog += "derived($first,$second)"
    }

    override fun score(): Int = first * 10 + second
}

fun box(): String {
    initializationLog.clear()
    val value: AbstractPairBase = PairScore(4, 2)

    if (initializationLog != listOf("base(4,2)", "derived(4,2)")) {
        return "FAIL1"
    }
    if (value.score() != 42) return "FAIL2"

    return "OK"
}
