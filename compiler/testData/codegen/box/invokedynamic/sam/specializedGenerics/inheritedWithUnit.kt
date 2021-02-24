// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFoo<T> {
    fun foo(): T
}

fun interface IFooUnit : IFoo<Unit> {
    override fun foo()
}

fun <T> fooT(iFoo: IFoo<T>) = iFoo.foo()
fun fooUnit(iFooUnit: IFooUnit) { iFooUnit.foo() }

var ok = "Failed"

fun box(): String {
    fooT { ok = "O" }
    fooUnit { ok += "K" }
    return ok
}
