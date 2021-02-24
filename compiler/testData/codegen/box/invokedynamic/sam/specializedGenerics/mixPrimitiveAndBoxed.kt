// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFooT<T> {
    fun foo(x: T): T
}

fun interface IFooInt {
    fun foo(x: Int): Int
}

fun interface IFooMixed0 : IFooInt, IFooT<Int>

fun interface IFooMixed1 : IFooInt, IFooT<Int> {
    override fun foo(x: Int): Int
}

fun box(): String {
    val f0 = IFooMixed0 { it * 2 }
    if (f0.foo(21) != 42)
        return "Failed: f0.foo(21)=${f0.foo(21)}"

    val f1 = IFooMixed1 { it * 2 }
    if (f1.foo(21) != 42)
        return "Failed: f1.foo(21)=${f1.foo(21)}"

    return "OK"
}

