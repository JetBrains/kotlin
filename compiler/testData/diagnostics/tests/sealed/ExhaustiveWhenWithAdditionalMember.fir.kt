// RUN_PIPELINE_TILL: BACKEND
sealed class Sealed(val x: Int) {
    data class Tuple(val x: Int, val y: Int)
    object First: Sealed(12)
    open class NonFirst(tuple: Tuple): Sealed(tuple.x) {
        val y: Int = tuple.y
        object Second: NonFirst(Tuple(34, 2))
        object Third: NonFirst(Tuple(56, 3))
    }
}

fun foo(s: Sealed): Int {
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when(s) {
        is Sealed.First -> 1
        is Sealed.NonFirst -> 0
        // no else required
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, isExpression, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
