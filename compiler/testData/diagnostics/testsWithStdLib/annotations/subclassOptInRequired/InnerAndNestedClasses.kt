// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

@RequiresOptIn
annotation class Api

@SubclassOptInRequired(Api::class)
open class B {
    open class C
    open inner class L
}

class E() : <!OPT_IN_TO_INHERITANCE_ERROR!>B<!>()
class K() : B.C()

fun test() {
    with(B()) {
        class Local : B.L()
    }
}

