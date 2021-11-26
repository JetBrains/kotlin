// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

fun interface IFooT<T> {
    fun foo(x: T): T
}

fun interface IFooIntArray {
    fun foo(x: IntArray): IntArray
}

fun interface IFooMix0 : IFooT<IntArray>, IFooIntArray

fun interface IFooMix1 : IFooT<IntArray>, IFooIntArray {
    override fun foo(x: IntArray): IntArray
}

fun box(): String {
    var t0 = "Failed 0"
    val f0 = IFooMix0 {
        t0 = "O" + it[0].toChar()
        it
    }
    f0.foo(intArrayOf('K'.toInt()))
    if (t0 != "OK")
        return "Failed: t0=$t0"

    var t1 = "Failed 1"
    val f1 = IFooMix1 {
        t1 = it[0].toChar() + "K"
        it
    }
    f1.foo(intArrayOf('O'.toInt()))
    if (t1 != "OK")
        return "Failed: t1=$t1"

    return "OK"
}