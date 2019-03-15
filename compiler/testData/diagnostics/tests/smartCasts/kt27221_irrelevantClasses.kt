// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

sealed class A
sealed class B : A()
sealed class C : A()
object BB : B()
object CC : C()

fun foo(a: A) {
    if (a is B) {
        if (a is C) {
            val t = when (<!DEBUG_INFO_SMARTCAST!>a<!>) {
                is CC -> "CC"
            }
        }
    }
}

fun foo2(a: A) {
    if (a is C) {
        if (a is B) {
            val t = <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_SMARTCAST!>a<!>) {
                    is CC -> "CC"
            }
        }
    }
}