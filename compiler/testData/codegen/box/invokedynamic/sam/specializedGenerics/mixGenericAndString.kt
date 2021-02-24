// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFooT<T> {
    fun foo(x: T): T
}

fun interface IFooStr {
    fun foo(x: String): String
}

fun interface IFooMix0 : IFooT<String>, IFooStr

fun interface IFooMix1 : IFooT<String>, IFooStr {
    override fun foo(x: String): String
}

fun box(): String {
    val f0 = IFooMix0 { "O" + it }
    if (f0.foo("K") != "OK")
        return "Failed: f0.foo(\"K\")=${f0.foo("K")}"

    val f1 = IFooMix1 { it + "K" }
    if (f1.foo("O") != "OK")
        return "Failed: f1.foo(\"O\")=${f1.foo("O")}"

    return "OK"
}