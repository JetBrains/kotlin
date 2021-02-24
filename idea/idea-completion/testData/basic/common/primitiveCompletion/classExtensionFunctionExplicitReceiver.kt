// FIR_COMPARISON
class A {
    fun Int.intExtFun() {}
    val Int.intExtVal: Int get() = 0
}

fun usage() {
    A().<caret>
}

// ABSENT: intExtFun
// ABSENT: intExtVal
