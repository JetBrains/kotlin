// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89434
error class Foo
error class Bar

sealed class Sealed {
    class A : Sealed()
    object B : Sealed()
    sealed class Nested : Sealed() {
        class C : Nested()
        class D : Nested()
    }
}

enum class Enum { X, Y }

fun sealedAll(x: Sealed | Foo): Int = when (x) {
    is Sealed.A -> 1
    Sealed.B -> 2
    is Sealed.Nested.C -> 3
    is Sealed.Nested.D -> 4
    is Foo -> 5
}

fun sealedNested(x: Sealed | Foo): Int = when (x) {
    is Sealed.A -> 1
    is Sealed.B -> 2
    is Sealed.Nested -> 3
    is Foo -> 4
}

fun sealedMissingInheritor(x: Sealed | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Sealed.A -> 1
    is Sealed.B -> 2
    is Foo -> 3
}

fun sealedMissingError(x: Sealed | Foo | Bar): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Sealed -> 1
    is Foo -> 2
}

fun sealedMissingMultiple(x: Sealed | Foo | Bar): Int = <!NO_ELSE_IN_WHEN("the 'B', 'is C', 'is D', 'is Bar' branches or an 'else' branch")!>when<!> (x) {
    is Sealed.A -> 1
    is Foo -> 2
}

fun sealedSupertype(x: Sealed | Foo): Int = when (x) {
    is Sealed -> 1
    is Foo -> 2
}

fun enumAll(x: Enum | Foo): Int = when (x) {
    Enum.X -> 1
    Enum.Y -> 2
    is Foo -> 3
}

fun enumMissingEntry(x: Enum | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    Enum.X -> 1
    is Foo -> 3
}

fun enumMissingError(x: Enum | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    Enum.X -> 1
    Enum.Y -> 2
}

fun enumIsCheck(x: Enum | Foo): Int = when (x) {
    is Enum -> 1
    is Foo -> 3
}

fun booleanAll(x: Boolean | Foo): Int = when (x) {
    true -> 1
    false -> 2
    is Foo -> 3
}

fun booleanMissing(x: Boolean | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    true -> 1
    is Foo -> 3
}

fun booleanMissingError(x: Boolean | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    true -> 1
    false -> 2
}

fun nullableSealed(x: Sealed? | Foo): Int = when (x) {
    is Sealed.A -> 1
    is Sealed.B -> 2
    is Sealed.Nested -> 3
    is Foo -> 4
    null -> 5
}

fun nullableSealedMissingNull(x: Sealed? | Foo): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    is Sealed.A -> 1
    is Sealed.B -> 2
    is Sealed.Nested -> 3
    is Foo -> 4
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
integerLiteral, isExpression, nestedClass, nullableType, objectDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
