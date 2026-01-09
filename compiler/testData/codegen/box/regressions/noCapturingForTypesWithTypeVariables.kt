// FILE: lib.kt
inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

// FILE: main.kt
fun foo(useScriptArgs: Array<out Any?>?) {
    val constructorArgs: Array<out Any?> = arrayOf(useScriptArgs.orEmpty())
}

fun box(): String {
    foo(arrayOf(1))
    return "OK"
}