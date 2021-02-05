// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IFooNStr {
    fun foo(x: Any): String?
}

fun interface IFooNN : IFooNStr {
    override fun foo(x: Any): Nothing?
}

fun box(): String {
    var r = "Failed"
    IFooNN {
        r = it.toString()
        null
    }.foo("OK")
    return r
}