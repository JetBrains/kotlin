// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class A(val a: Int)  {
    fun compareTo(other: A): Int = run {
        this.a - other.a
    }
}

fun case1() {
    val a3 = A(-1)
    val a4 = A(-3)

    val x = (a3 > a4)
}
