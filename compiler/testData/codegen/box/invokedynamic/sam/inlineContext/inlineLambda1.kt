// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
fun interface IFoo {
    fun foo()
}

fun foo(iFoo: IFoo) = iFoo.foo()

inline fun twice(fn: () -> Unit) {
    fn()
    fn()
}

fun box(): String {
    var test = 0
    twice {
        foo { test += 1 }
    }
    if (test != 2)
        return "Failed: test=$test"

    return "OK"
}