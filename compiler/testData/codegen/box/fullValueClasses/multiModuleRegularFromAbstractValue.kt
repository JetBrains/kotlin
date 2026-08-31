// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-84904

// MODULE: lib
// FILE: lib.kt

val initializationLog = mutableListOf<String>()

abstract value class AbstractValueBase(value: Int) {
    init {
        initializationLog += "base($value)"
    }

    abstract fun payload(): Int
}

// MODULE: main(lib)
// FILE: main.kt

class IdentityValue(private val value: Int) : AbstractValueBase(value) {
    init {
        initializationLog += "derived($value)"
    }

    override fun payload(): Int = value
}

fun box(): String {
    initializationLog.clear()
    val first: AbstractValueBase = IdentityValue(7)
    val second: AbstractValueBase = IdentityValue(7)

    if (initializationLog != listOf("base(7)", "derived(7)", "base(7)", "derived(7)")) {
        return "FAIL1"
    }
    if (first.payload() != 7 || second.payload() != 7) return "Fail2"
    if (first === second) return "FAIL3"

    return "OK"
}
