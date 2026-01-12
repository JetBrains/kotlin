// RUN_PIPELINE_TILL: BACKEND
// See KT-10648: Exhaustiveness check does not work with nested sealed hierarchy
sealed class Base {
    sealed class A : Base() {
        class A1 : A()
        class A2 : A()
    }
    sealed class B : Base() {
        class B1 : B()
        class B2 : B()
    }
}

fun foo(b: Base) = <!WHEN_ON_SEALED!>when (b) {
    is Base.A -> <!WHEN_ON_SEALED!>when(b) {
        is Base.A.A1 -> 1
        is Base.A.A2 -> 2
    }<!>
    is Base.B -> <!WHEN_ON_SEALED!>when(b) {
        is Base.B.B1 -> 3
        is Base.B.B2 -> 4
    }<!>
}<!>

fun bar(b: Base?) = if (b == null) 0 else <!WHEN_ON_SEALED!>when (b) {
    is Base.A -> <!WHEN_ON_SEALED!>when(b) {
        is Base.A.A1 -> 1
        is Base.A.A2 -> 2
    }<!>
    is Base.B -> <!WHEN_ON_SEALED!>when(b) {
        is Base.B.B1 -> 3
        is Base.B.B2 -> 4
    }<!>
}<!>

fun gav(b: Base?) = <!WHEN_ON_SEALED!>when (b) {
    null -> 0
    is Base.A -> <!WHEN_ON_SEALED!>when(b) {
        is Base.A.A1 -> 1
        is Base.A.A2 -> 2
    }<!>
    is Base.B -> <!WHEN_ON_SEALED!>when(b) {
        is Base.B.B1 -> 3
        is Base.B.B2 -> 4
    }<!>
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
isExpression, nestedClass, nullableType, sealed, smartcast, whenExpression, whenWithSubject */
