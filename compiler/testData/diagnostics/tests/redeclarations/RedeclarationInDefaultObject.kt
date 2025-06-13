// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A {
    companion object B {
        class <!REDECLARATION!>G<!>
        val <!REDECLARATION!>G<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, integerLiteral, nestedClass, objectDeclaration,
propertyDeclaration */
