// !DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE

interface SuperInterface

@kotlin.coroutines.experimental.RestrictsSuspension
open class RestrictedController : SuperInterface

class SubClass : RestrictedController()

suspend fun topLevel() {}

class A {
    suspend fun member() {}
}

fun generate1(f: suspend SuperInterface.() -> Unit) {}
fun generate2(f: suspend RestrictedController.() -> Unit) {}
fun generate3(f: suspend SubClass.() -> Unit) {}

fun A.test() {
    generate1 {
        topLevel()
        member()
        with(A()) {
            topLevel()
            member()
        }
    }
    generate2 {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        }
    }
    generate3 {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        }
    }

    <!WRONG_MODIFIER_TARGET!>suspend<!> fun SuperInterface.fun1() {
        topLevel()
        member()
        with(A()) {
            topLevel()
            member()
        }
    }
    <!WRONG_MODIFIER_TARGET!>suspend<!> fun RestrictedController.fun2() {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        }
    }
    <!WRONG_MODIFIER_TARGET!>suspend<!> fun SubClass.fun3() {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>topLevel<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>member<!>()
        }
    }

}

