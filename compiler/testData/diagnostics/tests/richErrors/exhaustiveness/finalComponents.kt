// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-89434
error class Foo
error class Bar
error object Baz

fun allComponents(x: String | Foo | Bar): Int = when (x) {
    is String -> 1
    is Foo -> 2
    is Bar -> 3
}

fun missingError(x: String | Foo | Bar): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
    is Foo -> 2
}

fun missingPrimary(x: String | Foo | Bar): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Foo -> 2
    is Bar -> 3
}

fun missingEverything(x: String | Foo | Bar): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
}

fun richErrorCoversAllErrors(x: String | Foo | Bar): Int = when (x) {
    is String -> 1
    is RichError -> 2
}

fun supertypeCoversPrimary(x: String | Foo | Bar): Int = when (x) {
    is CharSequence -> 1
    is Foo -> 2
    is Bar -> 3
}

fun negatedIsNotCounted(x: String | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    !is Foo -> 1
    <!USELESS_IS_CHECK!>is Foo<!> -> 2
}

fun redundantElse(x: String | Foo): Int = when (x) {
    is String -> 1
    is Foo -> 2
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 3
}

fun necessaryElse(x: String | Foo): Int = when (x) {
    is String -> 1
    else -> 3
}

fun statement(x: String | Foo) {
    when (x) {
        is String -> {}
    }
}

fun errorObject(x: String | Baz): Int = when (x) {
    is String -> 1
    Baz -> 2
}

fun errorObjectMissing(x: String | Baz): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
}

fun subjectVariable(f: () -> String | Foo): Int = when (val x = f()) {
    is String -> 1
    is Foo -> 0
}

fun orCondition(x: String | Foo | Bar): Int = when (x) {
    is String -> 1
    is Foo, is Bar -> 2
}

/* GENERATED_FIR_TAGS: classDeclaration, disjunctionExpression, equalityExpression, functionDeclaration, functionalType,
integerLiteral, isExpression, localProperty, objectDeclaration, propertyDeclaration, smartcast, whenExpression,
whenWithSubject */
