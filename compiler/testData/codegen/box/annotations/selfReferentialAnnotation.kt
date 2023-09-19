// FIR_DUMP
// DUMP_IR

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

fun box() = "OK"