val test1: (suspend () -> Unit)? = null
val test2: suspend (() -> Unit)? = null
val test3: suspend ( (() -> Unit)? ) = null

fun foo() {
    test1?.invoke()
    test2?.invoke()
    test3?.invoke()
}