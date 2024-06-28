// ISSUE: KT-21463
// SKIP_TXT

class Bound<T: Number>(val value: T)

fun test_1() {
    val b: Bound<in Int> = Bound(1)
    val vl: Number = <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(in kotlin.Int)")!>b.value<!>
}

fun test_2() {
    val b: Bound<*> = Bound(1)
    val vl: Number = <!DEBUG_INFO_EXPRESSION_TYPE("CapturedType(*)")!>b.value<!>
}
