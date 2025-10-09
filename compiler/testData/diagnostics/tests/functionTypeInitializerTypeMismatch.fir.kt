// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES

val f1: () -> Int = { <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f2: () -> Int = l@ {
    return@l <!RETURN_TYPE_MISMATCH("Int; String")!>""<!>
}
val f3: () -> Int = l@ {
    if (true) return@l 4
    return@l <!RETURN_TYPE_MISMATCH("Int; String")!>""<!>
}
val f5: () -> Int = <!INITIALIZER_TYPE_MISMATCH("Function0<Int>; Function1<??? (Unknown type for value parameter it), Int>")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> "" }<!>
val f6: () -> Int = <!INITIALIZER_TYPE_MISMATCH("Function0<Int>; Function1<Any, Int>")!>{ it: Any -> "" }<!>
val f7: () -> Int = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f8: String.() -> Int = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f9: String = <!INITIALIZER_TYPE_MISMATCH("String; Function0<String>")!>{  -> "" }<!>
val f10: Function0<Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f11: Function0<<!REDUNDANT_PROJECTION("Function0<out Int>")!>out<!> Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f12: Function0<<!CONFLICTING_PROJECTION("Function0<in Int>")!>in<!> Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f13: Function0<*> = {  -> "" }
val f14: Function<Int> = {  -> <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }
val f15: Function<Int> = { <!RETURN_TYPE_MISMATCH("Int; String")!>""<!> }

/* GENERATED_FIR_TAGS: functionalType, ifExpression, inProjection, integerLiteral, intersectionType, lambdaLiteral,
outProjection, propertyDeclaration, starProjection, stringLiteral, typeWithExtension */
