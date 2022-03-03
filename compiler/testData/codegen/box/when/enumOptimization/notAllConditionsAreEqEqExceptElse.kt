// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

enum class E {
    A, B, C, D;
}

val abc = setOf(E.A, E.B, E.C)

fun bar(): E = E.A

fun foo(e: E): String {
    val c = when (e) {
        E.B -> "B"
        in abc -> "OK"
        else -> "else"
    }
    return c
}

fun box() = foo(bar())

// CHECK_BYTECODE_TEXT
// 0 WhenMappings
// 0 TABLESWITCH