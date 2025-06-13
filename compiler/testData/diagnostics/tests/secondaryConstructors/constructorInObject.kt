// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
object A {
    <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
    init {}
}

enum class B {
    X() {
        <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
    }
}

class C {
    companion object {
        <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
    }
}

val anonObject = object {
    <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, enumDeclaration, enumEntry, init,
objectDeclaration, propertyDeclaration, secondaryConstructor */
