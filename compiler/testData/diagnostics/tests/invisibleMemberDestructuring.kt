// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58184

data class A(private val p1: Int, private val p2: Int)

fun test(a: A) {
    val (<!INVISIBLE_MEMBER!>p1<!>, <!INVISIBLE_MEMBER!>p2<!>) = a // ok, but INVISIBLE_MEMBER is expected
}
