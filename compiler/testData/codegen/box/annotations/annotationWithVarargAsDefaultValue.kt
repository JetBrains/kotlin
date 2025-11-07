// TARGET_BACKEND: JVM_IR

annotation class MyReplaceWith(val x: String, vararg val y: String)

annotation class MyDeprecated(
    val replaceWith: MyReplaceWith = MyReplaceWith(""),
)

fun getInt(x: String, vararg y: String): Int = 1

fun test(x: Int = getInt("")) {}

fun box() = "OK"
