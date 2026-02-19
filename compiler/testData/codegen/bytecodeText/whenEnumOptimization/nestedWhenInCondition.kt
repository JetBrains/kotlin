// IGNORE_BACKEND_K2: JVM_IR
// FIR_STATUS: FIR+JVM_IR generates TABLESWITCH for nested 'when' only, doesn't look critical.

// 2 (TABLE|LOOKUP)SWITCH

enum class ABCD {
    A, B, C, D
}

fun test(x: ABCD, y: ABCD, ok: String): String =
    when (x) {
        when (y) {
            ABCD.A -> ABCD.B
            ABCD.B -> ABCD.C
            ABCD.C -> ABCD.D
            ABCD.D -> ABCD.A
        } ->
            ok
        ABCD.A, ABCD.B, ABCD.C, ABCD.D ->
            x.toString()
    }
