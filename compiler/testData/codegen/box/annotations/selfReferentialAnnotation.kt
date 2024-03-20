// FIR_IDENTICAL
// FIR_DUMP
// DUMP_IR
// WITH_STDLIB

annotation class Ann(@Ann(1) val e: Int)

@MyRequiresOptIn("", MyRequiresOptIn.MyLevel.ERROR)
public annotation class MyRequiresOptIn(
    val a: String = "",
    @MyRequiresOptIn("", MyRequiresOptIn.MyLevel.WARNING) val b: MyLevel = MyLevel.ERROR
) {
    public enum class MyLevel {
        WARNING,
        ERROR,
    }
}

fun box(): String {
    val result = MyRequiresOptIn.MyLevel.values().joinToString()
    return when (result) {
        "WARNING, ERROR" -> "OK"
        else -> "Fail: $result"
    }
}
