// WITH_STDLIB
enum class ABCD {
    A, B, C, D
}

fun test(x: ABCD, y: ABCD, ok: String): String =
    when (x) {
        ABCD.A, ABCD.B ->
            when (y) {
                ABCD.A, ABCD.B -> ok
                ABCD.C, ABCD.D -> y.toString()
            }
        ABCD.C, ABCD.D ->
            x.toString()
    }

fun box(): String =
    test(ABCD.B, ABCD.A, "O") + test(ABCD.A, ABCD.B, "K")

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 (TABLE|LOOKUP)SWITCH
