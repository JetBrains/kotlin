// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER, -NOTHING_TO_INLINE

fun (()->Int).foo(y: String) {
    this()
}

inline fun (()->Int).bar(y: String) {
    this()
}

fun test1() {
    val x: String
    { <!UNINITIALIZED_VARIABLE!>x<!>.length }.foo(
        if (true) { x = ""; "" } else { x = ""; "" }
    )
}

fun test2() {
    val x: String
    { <!UNINITIALIZED_VARIABLE!>x<!>.length }.bar(
        if (true) { x = ""; "" } else { x = ""; "" }
    )
}