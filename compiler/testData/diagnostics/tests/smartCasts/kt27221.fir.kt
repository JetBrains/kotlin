// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

sealed class A
sealed class B : A()
sealed class C : B()
object BB : B()
object CC : C()

fun foo(a: A) {
    if (a is B) {
        if (a is C) {
            val t = when (a) {
                is CC -> "CC"
            }
        }
    }
}

fun foo2(a: A) {
    if (a is C) {
        if (<!USELESS_IS_CHECK!>a is B<!>) {
            val t = when (a) {
                is CC -> "CC"
            }
        }
    }
}
