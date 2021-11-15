// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// KT-42990

object O {
    val todo: String = TODO()

    fun test(): Int = Bar(todo.bar).result

    val String.bar: Int
        @JvmStatic
        get() = 42
}

class Bar(val result: Int)

fun box(): String = try {
    O.test()
    "Fail"
} catch (e: NotImplementedError) {
    "OK"
}
