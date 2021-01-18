// !DIAGNOSTICS: -UNUSED_PARAMETER -SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE
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
        extAny()
        memExtAny()
        extSuper()
        memExtSuper()

        ext()
        memExt()
        with(A()) {
            extAny()
            memExtAny()
            extSuper()
            memExtSuper()
            ext()
            memExt()
        }
    }
    generate3 {
        extAny()
        memExtAny()
        extSuper()
        memExtSuper()

        ext()
        memExt()
        extSub()
        memExtSub()
        with(A()) {
            extAny()
            memExtAny()
            extSuper()
            memExtSuper()
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
        extAny()
        memExtAny()
        extSuper()
        memExtSuper()

        ext()
        memExt()
        with(A()) {
            extAny()
            memExtAny()
            extSuper()
            memExtSuper()
            ext()
            memExt()
        }
    }
    suspend fun SubClass.fun3() {
        extAny()
        memExtAny()
        extSuper()
        memExtSuper()

        ext()
        memExt()
        extSub()
        memExtSub()
        with(A()) {
            extAny()
            memExtAny()
            extSuper()
            memExtSuper()
            ext()
            memExt()
            extSub()
            memExtSub()
        }
    }
}
