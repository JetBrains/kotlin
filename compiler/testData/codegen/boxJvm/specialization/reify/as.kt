// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize reified T> safeAs(x: Any?) = x as? T
fun <@JvmSpecialize reified T> safeAsNullable(x: Any?) = x as? T?
fun <@JvmSpecialize reified T> assertingAs(x: Any?) = x as T
fun <@JvmSpecialize reified T> assertingAsNullable(x: Any?) = x as T?

inline fun throws(cb: () -> Unit): Boolean {
    try {
        cb()
        return false
    } catch (err: Throwable) {
        return true
    }
}

fun box(): String {
    if (safeAs<Int>(42) != 42) return "fail: for safeAs<Int>(Int)"
    if (safeAsNullable<Int>(42) != 42) return "fail: for safeAsNullable<Int>(Int)"
    if (assertingAs<Int>(42) != 42) return "fail: for assertingAs<Int>(Int)"
    if (assertingAsNullable<Int>(42) != 42) return "fail: for assertingAsNullable<Int>(Int)"

    if (safeAs<String>("hello") != "hello") return "fail: for safeAs<String>(String)"
    if (safeAsNullable<String>("hello") != "hello") return "fail: for safeAsNullable<String>(String)"
    if (assertingAs<String>("hello") != "hello") return "fail: for assertingAs<String>(String)"
    if (assertingAsNullable<String>("hello") != "hello") return "fail: for assertingAsNullable<String>(String)"

    if (safeAs<Int>(null) != null) return "fail: for safeAs<Int>(null)"
    if (safeAsNullable<Int>(null) != null) return "fail: for safeAsNullable<Int>(null)"
    if (!throws { assertingAs<Int>(null) }) return "fail: for assertingAs<Int>(null)"
    if (assertingAsNullable<Int>(null) != null) return "fail: for assertingAsNullable<Int>(null)"

    if (safeAs<Int?>(null) != null) return "fail: for safeAs<Int?>(null)"
    if (safeAsNullable<Int?>(null) != null) return "fail: for safeAsNullable<Int?>(null)"
    if (assertingAs<Int?>(null) != null) return "fail: for assertingAs<Int?>(null)"
    if (assertingAsNullable<Int?>(null) != null) return "fail: for assertingAsNullable<Int?>(null)"

    if (safeAs<Int>("hello") != null) return "fail: for safeAs<Int>(String)"
    if (safeAsNullable<Int>("hello") != null) return "fail: for safeAsNullable<Int>(Sting)"
    if (!throws { assertingAs<Int>("hello") }) return "fail: for assertingAs<Int>(String)"
    if (!throws { assertingAsNullable<Int>("hello") }) return "fail: for assertingAsNullable<Int>(String)"

    return "OK"
}
