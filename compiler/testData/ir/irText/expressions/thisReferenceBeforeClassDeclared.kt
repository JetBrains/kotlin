// FIR_IDENTICAL
fun WithCompanion.test() {
    val test1 = object : WithCompanion(this) {}
    val test2 = object : WithCompanion(this.foo()) {}
}

open class WithCompanion(a: WithCompanion.Companion) {
    companion object {
        fun foo(): WithCompanion.Companion = this
    }
}