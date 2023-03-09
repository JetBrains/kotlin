// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57430

fun WithCompanion.test() {
    val test1 = object : WithCompanion(this) {}
    val test2 = object : WithCompanion(this.foo()) {}
}

open class WithCompanion(a: WithCompanion.Companion) {
    companion object {
        fun foo(): WithCompanion.Companion = this
    }
}
