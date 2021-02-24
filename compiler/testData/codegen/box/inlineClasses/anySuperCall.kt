// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM

inline class A(val x: Int) {
    fun f(): Int = super.hashCode()
}

fun box(): String {
    val a = A(1).f()
    return "OK"
}
