// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

fun doStuff(fn: String.() -> String) = "ok".fn()

fun box(): String {
    return doStuff(String::toUpperCase)
}
