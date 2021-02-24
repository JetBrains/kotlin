// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

inline class Z(val value: String?)

fun interface IFoo<T> {
    fun foo(x: T): T
}

fun foo1(fs: IFoo<Z>) = fs.foo(Z("O"))

fun box(): String {
    val t = foo1 { Z(it.value!! + "K") }
    if (t.value != "OK") return "Failed: t=$t"

    return "OK"
}