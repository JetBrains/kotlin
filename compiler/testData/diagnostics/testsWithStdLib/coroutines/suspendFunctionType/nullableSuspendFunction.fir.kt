val test1: (suspend () -> Unit)? = null
val test2: <!WRONG_MODIFIER_TARGET!>suspend<!> (() -> Unit)? = null
val test3: <!WRONG_MODIFIER_TARGET!>suspend<!> ( (() -> Unit)? ) = null

fun foo() {
    test1?.invoke()
    test2?.invoke()
    test3?.invoke()
}