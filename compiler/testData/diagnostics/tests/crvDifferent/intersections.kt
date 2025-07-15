// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1
// RETURN_VALUE_CHECKER_MODE: CHECKER

// FILE: Lib1.kt
interface Left {
    fun foo(): String
}

// MODULE: lib2
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: Lib2.kt
interface Right {
    fun foo(): String
}

// MODULE: main(lib1, lib2)
// RETURN_VALUE_CHECKER_MODE: FULL

// FILE: App.kt

interface X1: Left, Right

interface X2: Right, Left

fun main(x1: X1, x2: X2) {
    x1.foo()
    x2.foo()
}

fun smartCast1(i: Left) {
    if (i is Right) {
        i.foo() // (4) IntersectionOverride (IL & IR).foo(): String
    }
}

fun smartCast2(i: Right) {
    if (i is Left) {
        i.foo() // (4) IntersectionOverride (IL & IR).foo(): String
    }
}


fun <T> usage2(i: T) where T: Right, T: Left {
    i.foo()
}

fun <T> usage3(i: T) where T: Left, T: Right {
    i.foo()
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, enumDeclaration, enumEntry, functionDeclaration,
getter, localProperty, propertyDeclaration, setter, stringLiteral */
