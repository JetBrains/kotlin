// ISSUE: KT-37488

sealed class A

class B : A()
object C : A()

fun takeString(s: String) {}

fun test_1(a: A) {
    val s = when(a) {
        is B -> ""
        is C -> ""
    }
    takeString(s)
}

fun test_2(a: A) {
    val s = when(a) {
        is B -> ""
        C -> ""
    }
    takeString(s)
}