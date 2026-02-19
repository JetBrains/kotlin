// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

var f1: () -> Int = { <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f2: () -> Int = l@ {
    return@l <!RETURN_TYPE_MISMATCH("Int; String")!>""<!>
}
var f3: () -> Int = l@ {
    if (true) return@l 4
    return@l <!RETURN_TYPE_MISMATCH("Int; String")!>""<!>
}
var f5: () -> Int <!INITIALIZER_TYPE_MISMATCH("() -> Int; (??? (Unknown type for value parameter it)) -> Int")!>=<!> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> "" }
var f6: () -> Int <!INITIALIZER_TYPE_MISMATCH("() -> Int; (Any) -> Int")!>=<!> { it: Any -> "" }
var f7: () -> Int = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f8: String.() -> Int = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f9: String <!INITIALIZER_TYPE_MISMATCH("String; () -> String")!>=<!> {  -> "" }
var f10: Function0<Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f11: Function0<<!REDUNDANT_PROJECTION("() -> out Int")!>out<!> Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f12: Function0<<!CONFLICTING_PROJECTION("() -> in Int")!>in<!> Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f13: Function0<*> = {  -> "" }
var f14: Function<Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f15: Function<Int> = { <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
var f16: Function<Int> <!INITIALIZER_TYPE_MISMATCH("Function<Int>; Comparable<*> & Serializable")!>=<!> if (true) {
    f15
    1
} else "str"
var f17: Function<Int> <!INITIALIZER_TYPE_MISMATCH("Function<Int>; Any")!>=<!> when {
    1 == 1 -> true
    2 == 3 -> f15
    2 == 5 -> {
        f14
        false
    }
    else -> "str"
}

fun assign() {
    f1 = { <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f2 = l@ {
        return@l <!RETURN_TYPE_MISMATCH("Int; String")!>""<!>
    }

    f3 = l@ {
        if (true) return@l 4
        return@l <!RETURN_TYPE_MISMATCH("Int; String")!>""<!>
    }

    f5 <!ASSIGNMENT_TYPE_MISMATCH("() -> Int; (??? (Unknown type for value parameter it)) -> Int")!>=<!> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> "" }
    f6 <!ASSIGNMENT_TYPE_MISMATCH("() -> Int; (Any) -> Int")!>=<!> { it: Any -> "" }
    f7 = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f8 = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f9 <!ASSIGNMENT_TYPE_MISMATCH("String; () -> String")!>=<!> {  -> "" }
    f10 = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f11 = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f12 = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f13 = {  -> "" }
    f14 = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f15 = { <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
    f16 <!ASSIGNMENT_TYPE_MISMATCH("Function<Int>; Comparable<*> & Serializable")!>=<!> if (true) {
        f15
        1
    } else "str"
    f17 <!ASSIGNMENT_TYPE_MISMATCH("Function<Int>; Any")!>=<!> when {
        1 == 1 -> true
        2 == 3 -> f15
        2 == 5 -> {
            f14
            false
        }
        else -> "str"
    }
}

/* GENERATED_FIR_TAGS: functionalType, ifExpression, inProjection, integerLiteral, intersectionType, lambdaLiteral,
outProjection, propertyDeclaration, starProjection, stringLiteral, typeWithExtension */
