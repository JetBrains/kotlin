// RENDER_DIAGNOSTICS_FULL_TEXT
fun foo(z: (Int) -> String) {
    foo <!ARGUMENT_TYPE_MISMATCH("kotlin.String; kotlin.Unit")!>{}<!>
}