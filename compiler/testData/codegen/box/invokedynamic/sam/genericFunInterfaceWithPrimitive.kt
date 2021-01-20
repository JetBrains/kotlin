// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFoo<T> {
    fun foo(x: T): T
}

fun foo1(fs: IFoo<Int>) = fs.foo(1)

fun box(): String {
    val t = foo1 { it + 41 }
    if (t != 42) return "Failed: t=$t"

    return "OK"
}