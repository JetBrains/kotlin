// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89434
error class Foo
error class Bar

sealed class Sealed {
    class A : Sealed()
    class B : Sealed()
}

fun <T : Sealed> sealedBound(x: T | Foo): Int = when (x) {
    is Sealed.A -> 1
    is Sealed.B -> 2
    is Foo -> 3
}

fun <T : Sealed> sealedBoundMissing(x: T | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Sealed.A -> 1
    is Foo -> 3
}

fun <T : Any> anyBound(x: T | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Foo -> 1
}

fun <T : CharSequence> boundCovered(x: T | Foo): Int = when (x) {
    is CharSequence -> 1
    is Foo -> 2
}

fun <E : RichError> errorBound(x: String | E): Int = when (x) {
    is String -> 1
    is RichError -> 2
}

fun <E : RichError> errorBoundMissing(x: String | E): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
}

fun <E : Foo | Bar> errorUnionBound(x: String | E): Int = when (x) {
    is String -> 1
    is Foo -> 2
    is Bar -> 3
}

fun <E : Foo | Bar> errorUnionBoundMissing(x: String | E): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
    is Foo -> 2
}

fun <T : String | Foo> unionBound(x: T): Int = when (x) {
    is String -> 1
    is Foo -> 2
}

fun <T : String | Foo> unionBoundMissing(x: T): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is String -> 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, nestedClass, sealed,
smartcast, typeConstraint, typeParameter, whenExpression, whenWithSubject */
