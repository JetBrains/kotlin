// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFoo<T> {
    fun foo(): T
}

fun <T> foo(iFoo: IFoo<T>) = iFoo.foo()

var ok = "Failed"

fun box(): String {
    foo { ok = "OK" }
    return ok
}
