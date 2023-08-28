// FIR_DUMP
// DUMP_IR
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61386

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