// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

inline fun go(f: () -> String) = f()

fun String.id(): String = this

fun box(): String {
    val x = "OK"
    return go(x::id)
}
