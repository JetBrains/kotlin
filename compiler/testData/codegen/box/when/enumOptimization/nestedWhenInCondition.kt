// WITH_STDLIB
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

fun box(): String =
    test(ABCD.B, ABCD.A, "O") + test(ABCD.A, ABCD.D, "K")
