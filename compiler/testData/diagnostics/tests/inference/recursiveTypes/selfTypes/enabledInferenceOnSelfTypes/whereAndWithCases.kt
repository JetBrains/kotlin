// FIR_IDENTICAL
// ISSUE: KT-59012
// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

interface I1<G : I1<G>> {
    fun <T : G> foo() : T
    fun <T : G> bar(vararg args: T)
}

interface I2<in G : I2<G>> {
    fun <T : G> foo() : T
    fun <T : G> bar(vararg args: T)
}

interface I3<G> where G : I3<G> {
    fun <T> foo() : T where T : G
    fun <T> bar(vararg args: T) where T : G
}

fun test(a: I2<*>, b: I3<*>) {
    val x = a.foo()
    <!DEBUG_INFO_EXPRESSION_TYPE("I2<*>")!>x<!>
    a.bar()
    val y = b.foo()
    <!DEBUG_INFO_EXPRESSION_TYPE("I3<*>")!>y<!>
    b.bar()
}

fun withTest(a: I1<*>, b: I3<*>) {
    with(a) {
        val x = foo()
        <!DEBUG_INFO_EXPRESSION_TYPE("I1<*>")!>x<!>
        bar()
    }
    with(b) {
        val y = foo()
        <!DEBUG_INFO_EXPRESSION_TYPE("I3<*>")!>y<!>
        bar()
    }
}
