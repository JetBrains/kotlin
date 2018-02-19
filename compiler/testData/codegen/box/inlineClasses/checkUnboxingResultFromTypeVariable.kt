// !LANGUAGE: +InlineClasses

inline class Result<T>(val a: Any?) {
    fun typed(): T = a as T
}

fun box(): String {
    val asInt = Result<Int>(19)
    val asString = Result<String>("sample")
    val asResult = Result<Result<Int>>(asInt)

    val unboxedInt = asInt.typed()
    val unboxedString = asString.typed()
    val unboxedResult = asResult.typed()

    if (unboxedInt != 19) return "fail"
    if (unboxedString != "sample") return "fail"
    if (unboxedResult.typed() != 19) return "fail"

    if (asResult.typed().typed() != 19) return "fail"

    return "OK"
}