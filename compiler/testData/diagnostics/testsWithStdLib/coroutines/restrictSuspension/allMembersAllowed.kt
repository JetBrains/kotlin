// !DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE

interface SuperInterface {
    suspend fun superFun() {}
    suspend fun String.superExtFun() {}
}

@kotlin.coroutines.experimental.RestrictsSuspension
open class RestrictedController : SuperInterface {
    suspend fun memberFun() {}
    suspend fun String.memberExtFun() {}
}

class SubClass : RestrictedController() {
    suspend fun subFun() {}
    suspend fun String.subExtFun() {}
}

fun generate1(f: suspend SuperInterface.() -> Unit) {}
fun generate2(f: suspend RestrictedController.() -> Unit) {}
fun generate3(f: suspend SubClass.() -> Unit) {}

fun String.test() {
    generate1 {
        superFun()
        superExtFun()
        with("") {
            superFun()
            superExtFun()
        }
    }
    generate2 {
        superFun()
        superExtFun()
        memberFun()
        memberExtFun()
        with("") {
            superFun()
            superExtFun()
            memberFun()
            memberExtFun()
        }
    }
    generate3 {
        superFun()
        superExtFun()
        memberFun()
        memberExtFun()
        subFun()
        superExtFun()
        with("") {
            superFun()
            superExtFun()
            memberFun()
            memberExtFun()
            subFun()
            superExtFun()
        }
    }

    <!WRONG_MODIFIER_TARGET!>suspend<!> fun SuperInterface.fun1() {
        superFun()
        superExtFun()
        with("") {
            superFun()
            superExtFun()
        }
    }
    <!WRONG_MODIFIER_TARGET!>suspend<!> fun RestrictedController.fun2() {
        superFun()
        superExtFun()
        memberFun()
        memberExtFun()
        with("") {
            superFun()
            superExtFun()
            memberFun()
            memberExtFun()
        }
    }
    <!WRONG_MODIFIER_TARGET!>suspend<!> fun SubClass.fun3() {
        superFun()
        superExtFun()
        memberFun()
        memberExtFun()
        subFun()
        superExtFun()
        with("") {
            superFun()
            superExtFun()
            memberFun()
            memberExtFun()
            subFun()
            superExtFun()
        }
    }

}

