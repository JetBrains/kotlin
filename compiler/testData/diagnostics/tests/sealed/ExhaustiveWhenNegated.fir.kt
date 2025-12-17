// RUN_PIPELINE_TILL: BACKEND
sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
    }
}

fun foo(s: Sealed): Int {
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when(s) {
        is Sealed.First -> 1
        !is Sealed.First -> 0
        // no else required
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
