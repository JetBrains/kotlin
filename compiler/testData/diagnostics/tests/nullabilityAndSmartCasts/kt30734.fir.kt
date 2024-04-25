// DIAGNOSTICS: -UNUSED_VARIABLE
// Issue: KT-30734

class Sample {
    fun foo(): Boolean = true
}

fun test(ls: Sample?) {
    val filter: () -> Boolean = if (ls == null) {
        { false }
    } else {
        { ls.foo() } // OK in OI, error in NI
    }
}