// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
fun interface IFoo {
    fun foo()
}

fun foo(iFoo: IFoo) = iFoo.foo()

inline fun twice(fn: () -> Unit) {
    fn()
    fn()
}

// FILE: 2.kt
fun box(): String {
    var test = 0
    twice {
        foo { test += 1 }
    }
    if (test != 2)
        return "Failed: test=$test"

    return "OK"
}