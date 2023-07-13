// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

fun <K> id(x: K): K = x

fun box(): String {
    val x4: String.() -> String = if (true) {fun String.(): String { return "this" }} else {{ str: String -> "that" }}
    val x51: String.() -> String = if (false) {fun String.(): String { return "this" }} else {{ "that" }}
    val x52: String.() -> String = if (false) {fun String.(): String { return "this" }} else {{ str: String -> "that" }}
    val x53: String.() -> String = if (false) {fun String.(): String { return "this" }} else {fun(str: String) = "that"}

    val i28: Int.(String) -> Int = id { i: Int, s -> i + s.length }

    val result = "test".x4() +
            "::" + "test".x51() +
            "::" + "test".x52() +
            "::" + "test".x53() +
            "::" + 10.i28("test")

    return if (result == "this::that::that::that::14") {
        "OK"
    } else {
        "Fail: $result"
    }
}
