// WITH_REFLECT

fun doStuff(fn: String.() -> String) = "ok".fn()

fun box(): String {
    return doStuff(String::toUpperCase)
}
