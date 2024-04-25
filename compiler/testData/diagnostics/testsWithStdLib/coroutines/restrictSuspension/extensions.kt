// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE
interface SuperInterface

@kotlin.coroutines.RestrictsSuspension
open class RestrictedController : SuperInterface

class SubClass : RestrictedController()

suspend fun Any?.extAny() {}
suspend fun SuperInterface.extSuper() {}
suspend fun RestrictedController.ext() {}
suspend fun SubClass.extSub() {}

class A {
    suspend fun Any?.memExtAny() {}
    suspend fun SuperInterface.memExtSuper() {}
    suspend fun RestrictedController.memExt() {}
    suspend fun SubClass.memExtSub() {}
}


fun generate1(f: suspend SuperInterface.() -> Unit) {}
fun generate2(f: suspend RestrictedController.() -> Unit) {}
fun generate3(f: suspend SubClass.() -> Unit) {}

fun A.test() {
    generate1 {
        extAny()
        memExtAny()
        extSuper()
        memExtSuper()
        with(A()) {
            extAny()
            memExtAny()
            extSuper()
            memExtSuper()
        }
    }
    generate2 {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()

        ext()
        memExt()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()
            ext()
            memExt()
        }
    }
    generate3 {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()

        ext()
        memExt()
        extSub()
        memExtSub()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()
            ext()
            memExt()
            extSub()
            memExtSub()
        }
    }

    suspend fun SuperInterface.fun1() {
        extAny()
        memExtAny()
        extSuper()
        memExtSuper()
        with(A()) {
            extAny()
            memExtAny()
            extSuper()
            memExtSuper()
        }
    }
    suspend fun RestrictedController.fun2() {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()

        ext()
        memExt()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()
            ext()
            memExt()
        }
    }
    suspend fun SubClass.fun3() {
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
        <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()

        ext()
        memExt()
        extSub()
        memExtSub()
        with(A()) {
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtAny<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>extSuper<!>()
            <!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>memExtSuper<!>()
            ext()
            memExt()
            extSub()
            memExtSub()
        }
    }
}
