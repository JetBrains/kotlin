// FIR_IDENTICAL

val <T : CharSequence> T.gk: () -> T
    get() = { -> this }

fun testGeneric1(x: String) = x.gk()

val <T> T.kt26531Val: () -> T
    get() = fun () = this

fun kt26531() = 7.kt26531Val()
