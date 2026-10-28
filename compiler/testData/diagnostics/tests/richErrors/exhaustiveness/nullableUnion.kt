// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89434
error class Foo
error class Bar

fun nullablePrimary(x: String? | Foo): Int = when (x) {
    is String -> 1
    is Foo -> 2
    null -> 3
}

fun nullablePrimaryMissingNull(x: String? | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
    is Foo -> 2
}

fun nullablePrimaryMissingError(x: String? | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
    null -> 3
}

fun nullableIsCheck(x: String? | Foo): Int = when (x) {
    is String? -> 1
    is Foo -> 2
}

fun nullableIsCheck2(x: (String | Foo | Bar)?): Int = when (x) {
    is String -> 0
    is RichError -> 1
    null -> 2
}

fun nullableUnion(x: (Foo | Bar)?): Int = when (x) {
    is Foo -> 1
    is Bar -> 2
    null -> 3
}

fun nullableUnionMissingNull(x: (Foo | Bar)?): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Foo -> 1
    is Bar -> 2
}

fun nullableUnionMissingError(x: (Foo | Bar)?): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Foo -> 1
    null -> 3
}

fun richErrorCoversNullableUnion(x: (Foo | Bar)?): Int = when (x) {
    is RichError -> 1
    null -> 2
}

fun nullableNothingPrimary(x: Nothing? | Foo | Bar): Int = when (x) {
    null -> 1
    is Foo -> 2
    is Bar -> 3
}

fun onlyErrors(x: Foo | Bar): Int = when (x) {
    is Foo -> 1
    is Bar -> 2
}

fun onlyErrorsMissing(x: Foo | Bar): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Foo -> 1
}

fun onlyErrorsNullNotNeeded(x: Foo | Bar): Int = when (x) {
    is Foo -> 1
    is Bar -> 2
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 3
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, isExpression,
nullableType, smartcast, whenExpression, whenWithSubject */
