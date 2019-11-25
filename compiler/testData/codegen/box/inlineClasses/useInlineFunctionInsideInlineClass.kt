// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Foo(val a: String) {
    fun test(): String {
        return a + inlineFun()
    }
}

inline fun inlineFun(): String = "K"

fun box(): String {
    val f = Foo("O")
    return f.test()
}