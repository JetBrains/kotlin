// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class Api

@SubclassOptInRequired(Api::class)
open class B {
    open class C
    open inner class L
}

class E() : <!OPT_IN_USAGE_ERROR!>B<!>()
class K() : B.C()

fun test() {
    with(B()) {
        class Local : B.L()
    }
}

