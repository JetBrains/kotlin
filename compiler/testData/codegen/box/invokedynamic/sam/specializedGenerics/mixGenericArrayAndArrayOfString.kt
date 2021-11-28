// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

fun interface IFooT<T> {
    fun foo(x: Array<T>): T
}

fun interface IFooStr {
    fun foo(x: Array<String>): String
}

fun interface IFooMix0 : IFooT<String>, IFooStr

fun interface IFooMix1 : IFooT<String>, IFooStr {
    override fun foo(x: Array<String>): String
}

fun box(): String {
    val f0 = IFooMix0 { "O" + it[0] }
    val t0 = f0.foo(arrayOf("K"))
    if (t0 != "OK")
        return "Failed: t0=$t0"

    val f1 = IFooMix1 { it[0] + "K" }
    val t1 = f1.foo(arrayOf("O"))
    if (t1 != "OK")
        return "Failed: t1=$t1"

    return "OK"
}