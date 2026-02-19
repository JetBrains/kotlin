// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_STDLIB
// LANGUAGE: +ContextParameters

// FILE: lib.kt
class A {
    val b = 42

    context(c: Int)
    inline fun <T : CharSequence> A.foo(a: T) = a.length + b + c + this.b

    context(c: Int)
    fun <T : CharSequence> A.bar(a: T) = a.length + b + c + this.b
}

// FILE: main.kt
fun box(): String = with(42) {
    val arguments = listOf<String>("0123456789", "", "\n")
    for (arg in arguments)
        if (A().run { foo(arg) } != A().run { bar(arg) })
            return arg
    return "OK"
}