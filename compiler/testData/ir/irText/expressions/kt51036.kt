// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// SKIP_KT_DUMP

class A {
    companion object;
    operator fun String.invoke() = Unit
    fun close() = synchronized(this) { "Abc" }()
}
