// RUN_PIPELINE_TILL: BACKEND
sealed class Sealed {
    object First: Sealed()
    sealed class NonFirst {
        object Second: NonFirst()
        object Third: NonFirst()
        object Fourth: Sealed()
    }    
}

fun foo(s: Sealed, nf: Sealed.NonFirst): Int {
    val si = <!WHEN_ON_SEALED_GEEN_ELSE!>when(s) {
        Sealed.First -> 1
        Sealed.NonFirst.Fourth -> 4
    }<!>
    val nfi = <!WHEN_ON_SEALED_GEEN_ELSE!>when(nf) {
        Sealed.NonFirst.Second -> 2
        Sealed.NonFirst.Third -> 3
    }<!>
    return si + nfi
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, equalityExpression, functionDeclaration, integerLiteral,
localProperty, nestedClass, objectDeclaration, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
