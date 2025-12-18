// RUN_PIPELINE_TILL: BACKEND
sealed class Sealed() {
    object First: Sealed()
    open class NonFirst: Sealed() {
        object Second: NonFirst()
        object Third: NonFirst()
        // It's ALLOWED to inherit Sealed also from here
        object Fourth: Sealed()
    }    
}

fun foo(s: Sealed) = <!WHEN_ON_SEALED_GEEN_ELSE!>when(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.Fourth -> 4
    // no else required
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, isExpression,
nestedClass, objectDeclaration, primaryConstructor, sealed, smartcast, whenExpression, whenWithSubject */
