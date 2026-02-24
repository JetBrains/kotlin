// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-82579
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:2.2.0,2.3.0
// ^^^ Context parameter specificity rules have been chaged in 2.3.20-Beta1, so `box()` function returns a different value starting with 2.3.20.

open class A
open class B : A()
class C : B()

context(_: A) fun foo0(a: B = B()) = "X"
fun foo0(b: C = C()) = "OK"

context(ctx: A) fun foo1(a: B = B()) = "X"
fun foo1() = "OK"

context(_: A) fun foo2(vararg a: B) = "X"
fun foo2() = "OK"

fun box(): String {
    context(A()) {
        if (foo0(C()) != "OK") return "fail 0"
        if (foo1() != "OK") return "fail 1"
        if (foo2() != "OK") return "fail 2"
    }
    return "OK"
}
