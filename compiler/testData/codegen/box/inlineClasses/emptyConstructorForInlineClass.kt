// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Foo(val s: String) {
    fun asResult(): String = s
}

fun box(): String {
    val a = Foo("OK")
    return a.asResult()
}