// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

interface In<in E>

enum class A : In<A> {
    X, Y
}

// FILE: main.kt
import test.A
import test.In

fun <T : In<T>> id(x: T): T = TODO()

fun main() {
    // T <: In<T>
    //
    // A <: T
    //  => A <: In<T>
    //  => In<A> <: In<T>
    //  => T <: A
    //  => T == A
    id(A.X)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, in, interfaceDeclaration, nullableType,
typeConstraint, typeParameter */
