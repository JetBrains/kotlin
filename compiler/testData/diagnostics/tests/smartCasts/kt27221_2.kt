// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

sealed class A
sealed class B : A()
sealed class C : B()
sealed class D : C()
object BB : B()
object CC : C()
object DD : D()

fun foo1(a: A) {
    if (a is B) {
        if (a is D) {
            if (<!USELESS_IS_CHECK!>a is C<!>) {
                val t =
                    when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
                        is DD -> "DD"
                    }
            }
        }
    }
}

fun foo2(a: A) {
    if (a is B) {
        if (a is D) {
            if (<!USELESS_IS_CHECK!>a is C<!>) {
                val t =
                    when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
                        is DD -> "DD"
                    }
            }
        }
    }
}