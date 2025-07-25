// RUN_PIPELINE_TILL: BACKEND
// MODULE: library
// FILE: Base.kt
package test

sealed interface IBase

sealed class Base

// FILE: A.kt
package test

interface IA : IBase

// FILE: B.kt
package test

sealed class B : Base(), IBase {
    class First : B()
    class Second : B()
}

// FILE: C.kt
package test

enum class C : IBase {
    SomeValue, AnotherValue
}

// FILE: D.kt
package test

object D : Base(), IBase

// MODULE: main(library)
// FILE: main.kt
import test.*

fun test_1(base: IBase) {
    val x = when (base) {
        is IA -> 1
        is B -> 2
        is C -> 3
        is D -> 4
    }
}

fun test_2(base: IBase) {
    val x = when (base) {
        is IA -> 1
        is B.First -> 2
        is B.Second -> 3
        C.SomeValue -> 4
        C.AnotherValue -> 5
        D -> 6
    }
}

fun test_3(base: Base) {
    val x = when (base) {
        is B -> 2
        is D -> 4
    }
}

fun test_4(base: Base) {
    val x = when (base) {
        is B.First -> 2
        is B.Second -> 3
        D -> 6
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
integerLiteral, interfaceDeclaration, isExpression, localProperty, nestedClass, objectDeclaration, propertyDeclaration,
sealed, smartcast, whenExpression, whenWithSubject */
