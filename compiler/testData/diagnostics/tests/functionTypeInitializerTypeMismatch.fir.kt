// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES

val f1: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function0<kotlin.String>")!>{ "" }<!>
val f2: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function0<kotlin.String>")!>l@ {
    return@l ""
}<!>
val f3: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function0<kotlin.Comparable<kotlin.Int & kotlin.String> & java.io.Serializable>")!>l@ {
    if (true) return@l 4
    return@l ""
}<!>
val f5: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function1<ERROR CLASS: Cannot infer type for parameter it, kotlin.String>")!>{ <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> "" }<!>
val f6: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function1<kotlin.Any, kotlin.String>")!>{ it: Any -> "" }<!>
val f7: () -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function0<kotlin.String>")!>{  -> "" }<!>
val f8: String.() -> Int = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function1<kotlin.String, kotlin.Int>; kotlin.Function1<kotlin.String, kotlin.String>")!>{  -> "" }<!>
val f9: String = <!INITIALIZER_TYPE_MISMATCH("kotlin.String; kotlin.Function0<kotlin.String>")!>{  -> "" }<!>
val f10: Function0<Int> = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<kotlin.Int>; kotlin.Function0<kotlin.String>")!>{  -> "" }<!>
val f11: Function0<<!REDUNDANT_PROJECTION("kotlin.Function0<out kotlin.Int>")!>out<!> Int> = <!INITIALIZER_TYPE_MISMATCH("kotlin.Function0<out kotlin.Int>; kotlin.Function0<kotlin.String>")!>{  -> "" }<!>
val f12: Function0<<!CONFLICTING_PROJECTION("kotlin.Function0<in kotlin.Int>")!>in<!> Int> = {  -> "" }
val f13: Function0<*> = {  -> "" }
