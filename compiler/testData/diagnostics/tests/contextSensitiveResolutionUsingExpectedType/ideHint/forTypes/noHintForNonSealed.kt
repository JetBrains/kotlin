// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

open class B {
    class X : B()
    class Y : B()
}

// FILE: main.kt
import test.B

fun foo(b: B) {
    // No hint because B is not sealed
    if (b is B.X) {
        "".hashCode()
    }

    val x = b as B.X
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
nestedClass, propertyDeclaration, stringLiteral */
