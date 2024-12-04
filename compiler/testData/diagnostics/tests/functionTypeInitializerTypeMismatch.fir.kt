// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES

val f1: () -> Int = { <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
val f2: () -> Int = l@ {
    return@l <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!>
}
val f3: () -> Int = l@ {
    if (true) return@l 4
    return@l <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!>
}
val f5: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function1<ERROR CLASS: Cannot infer type for parameter it, kotlin.String>")!>{ <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> "" }<!>
val f6: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function1<kotlin.Any, kotlin.String>")!>{ it: Any -> "" }<!>
val f7: () -> Int = {  -> <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
val f8: String.() -> Int = {  -> <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
val f9: String = <!INITIALIZER_TYPE_MISMATCH("kotlin.String; kotlin.Function0<kotlin.String>")!>{  -> "" }<!>
val f10: Function0<Int> = {  -> <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
val f11: Function0<<!REDUNDANT_PROJECTION("kotlin.Function0<out kotlin.Int>")!>out<!> Int> = {  -> <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
val f12: Function0<<!CONFLICTING_PROJECTION("kotlin.Function0<in kotlin.Int>")!>in<!> Int> = {  -> "" }
val f13: Function0<*> = {  -> "" }
val f14: Function<Int> = {  -> <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
val f15: Function<Int> = { <!RETURN_TYPE_MISMATCH("kotlin.Int; kotlin.String")!>""<!> }
