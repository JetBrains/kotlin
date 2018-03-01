// !LANGUAGE: +InlineClasses

inline class Result<T>(val a: Any?) {
    fun typed(): T = a as T
}

fun <T> takeResult(r: Result<T>) {}
fun takeResultOfInt(r: Result<Int>) {}
fun takeInt(i: Int) {}


fun box(): String {
    val asInt = Result<Int>(19)
    val asString = Result<String>("sample")
    val asResult = Result<Result<Int>>(asInt)
    val asResultCtor = Result<Result<Int>>(Result<Int>(10))

    takeResult(asInt)
    takeResult(asString)
    takeResult(asResult)
    takeResult(asResultCtor)

    takeResultOfInt(asInt)
    takeInt(asInt.typed())

    val unboxedInt = asInt.typed()
    val unboxedString = asString.typed()
    val unboxedResult = asResult.typed()
    val unboxedAsCtor = asResultCtor.typed()

    if (unboxedInt != 19) return "fail"
    if (unboxedString != "sample") return "fail"
    if (unboxedResult.typed() != 19) return "fail"
    if (unboxedAsCtor.typed() != 10) return "fail"

    if (asResult.typed().typed() != 19) return "fail"
    if (asResultCtor.typed().typed() != 10) return "fail"

    return "OK"
}