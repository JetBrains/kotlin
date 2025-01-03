// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
fun foo(z: (Int) -> String) {
    foo <!ARGUMENT_TYPE_MISMATCH("kotlin.Function1<kotlin.Int, kotlin.Unit>; kotlin.Function1<kotlin.Int, kotlin.String>")!>{}<!>
}
