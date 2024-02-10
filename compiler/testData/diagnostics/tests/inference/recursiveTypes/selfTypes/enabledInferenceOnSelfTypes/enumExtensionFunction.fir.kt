// ISSUE: KT-59012
// !LANGUAGE: +TypeInferenceOnCallsWithSelfTypes

fun <G : Enum<G>, T : G> Enum<G>.foo(): T = TODO()
fun <G : Enum<G>, T : G> Enum<G>.bar(vararg args: T): Unit = TODO()

fun <G : Enum<G>> enumTest(a: Enum<G>) {
    val x = <!DEBUG_INFO_EXPRESSION_TYPE("G")!>a.foo()<!>
    x
    a.bar()
}

fun enumStarTest(a: Enum<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(*)")!>a.foo()<!>
    a.bar()
}
