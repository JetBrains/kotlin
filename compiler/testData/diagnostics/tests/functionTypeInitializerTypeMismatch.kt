// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// RENDER_DIAGNOSTICS_MESSAGES

val f1: () -> Int = { <!TYPE_MISMATCH!>""<!> }
val f2: () -> Int = l@ {
    return@l <!TYPE_MISMATCH!>""<!>
}
val f3: () -> Int = l@ {
    if (true) return@l 4
    return@l <!TYPE_MISMATCH!>""<!>
}
val f5: () -> Int = { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it<!> -> <!TYPE_MISMATCH!>""<!> }
val f6: () -> Int = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it: Any<!> -> <!TYPE_MISMATCH!>""<!> }
val f7: () -> Int = {  -> <!TYPE_MISMATCH!>""<!> }
val f8: String.() -> Int = {  -> <!TYPE_MISMATCH!>""<!> }
val f9: String = <!TYPE_MISMATCH!>{  -> "" }<!>
val f10: Function0<Int> = {  -> <!TYPE_MISMATCH!>""<!> }
val f11: Function0<<!REDUNDANT_PROJECTION!>out<!> Int> = {  -> <!TYPE_MISMATCH!>""<!> }
val f12: Function0<<!CONFLICTING_PROJECTION!>in<!> Int> = {  -> <!TYPE_MISMATCH!>""<!> }
val f13: Function0<*> = {  -> "" }
val f14: Function<Int> = <!TYPE_MISMATCH!>{  -> "" }<!>
val f15: Function<Int> = <!TYPE_MISMATCH!>{ "" }<!>
