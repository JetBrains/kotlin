// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES

var f1: () -> Int = { <!TYPE_MISMATCH!>""<!> }
var f2: () -> Int = l@ {
    return@l <!TYPE_MISMATCH!>""<!>
}
var f3: () -> Int = l@ {
    if (true) return@l 4
    return@l <!TYPE_MISMATCH!>""<!>
}
var f5: () -> Int = { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it<!> -> <!TYPE_MISMATCH!>""<!> }
var f6: () -> Int = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it: Any<!> -> <!TYPE_MISMATCH!>""<!> }
var f7: () -> Int = {  -> <!TYPE_MISMATCH!>""<!> }
var f8: String.() -> Int = {  -> <!TYPE_MISMATCH!>""<!> }
var f9: String = <!TYPE_MISMATCH!>{  -> "" }<!>
var f10: Function0<Int> = {  -> <!TYPE_MISMATCH!>""<!> }
var f11: Function0<<!REDUNDANT_PROJECTION!>out<!> Int> = {  -> <!TYPE_MISMATCH!>""<!> }
var f12: Function0<<!CONFLICTING_PROJECTION!>in<!> Int> = {  -> <!TYPE_MISMATCH!>""<!> }
var f13: Function0<*> = {  -> "" }
var f14: Function<Int> = <!TYPE_MISMATCH!>{  -> "" }<!>
var f15: Function<Int> = <!TYPE_MISMATCH!>{ "" }<!>
var f16: Function<Int> = if (true) <!TYPE_MISMATCH!>{
    f15
    1
}<!> else <!TYPE_MISMATCH!>"str"<!>
var f17: Function<Int> = when {
    1 == 1 -> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>
    2 == 3 -> f15
    2 == 5 -> <!TYPE_MISMATCH!>{
        f14
        false
    }<!>
    else -> <!TYPE_MISMATCH!>"str"<!>
}

fun assign() {
    f1 = { <!TYPE_MISMATCH!>""<!> }
    f2 = l@ {
        return@l <!TYPE_MISMATCH!>""<!>
    }

    f3 = l@ {
        if (true) return@l 4
        return@l <!TYPE_MISMATCH!>""<!>
    }

    f5 = { <!CANNOT_INFER_PARAMETER_TYPE, EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it<!> -> <!TYPE_MISMATCH!>""<!> }
    f6 = { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>it: Any<!> -> <!TYPE_MISMATCH!>""<!> }
    f7 = {  -> <!TYPE_MISMATCH!>""<!> }
    f8 = {  -> <!TYPE_MISMATCH!>""<!> }
    f9 = <!TYPE_MISMATCH!>{  -> "" }<!>
    f10 = {  -> <!TYPE_MISMATCH!>""<!> }
    f11 = {  -> <!TYPE_MISMATCH!>""<!> }
    f12 = {  -> <!TYPE_MISMATCH!>""<!> }
    f13 = {  -> "" }
    f14 = <!TYPE_MISMATCH!>{  -> "" }<!>
    f15 = <!TYPE_MISMATCH!>{ "" }<!>
    f16 = if (true) <!TYPE_MISMATCH!>{
        f15
        1
    }<!> else <!TYPE_MISMATCH!>"str"<!>
    f17 = when {
        1 == 1 -> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>
        2 == 3 -> f15
        2 == 5 -> <!TYPE_MISMATCH!>{
            f14
            false
        }<!>
        else -> <!TYPE_MISMATCH!>"str"<!>
    }
}

/* GENERATED_FIR_TAGS: functionalType, ifExpression, inProjection, integerLiteral, intersectionType, lambdaLiteral,
outProjection, propertyDeclaration, starProjection, stringLiteral, typeWithExtension */
