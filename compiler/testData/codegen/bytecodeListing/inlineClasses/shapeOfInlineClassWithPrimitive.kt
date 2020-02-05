// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Foo(val l: Long) {
    fun empty() {}
    fun param(x: Double) {}
    fun Any.extension(y: String) {}
}