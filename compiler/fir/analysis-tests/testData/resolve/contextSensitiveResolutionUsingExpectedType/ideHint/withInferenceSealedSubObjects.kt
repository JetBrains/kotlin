// RUN_PIPELINE_TILL: BACKEND
// IDE_MODE

// FILE: test.kt
package test

sealed class A {
    object X : A()
    object Y : A()
}

// FILE: main.kt
import test.A

fun expectsA(x: A) {}
fun expectsAny(x: Any) {}

fun expectAInLambda(x: () -> A) {}

fun <T1> id(x: T1): T1 = x
fun <T2> myListOf(x: T2): List<T2> = TODO()

fun <T3> select(x: T3, y: T3): T3 = TODO()

fun <T4> myRun(x: () -> T4): T4 = TODO()

fun main() {
    // Currently, we don't suggest to convert it to CSR because it changes semantics
    // (currently `id` call is inferred to `A.X` type, but for CSR it would be inferred to `A` type)
    val a1: A = id(A.X)
    val a2 = id(A.X)
    val list1: List<A> = myListOf(A.Y)
    val list2 = myListOf(A.Y)

    expectsA(id(A.X))
    expectsAny(id(A.X))

    expectAInLambda {
        <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>
    }

    expectAInLambda {
        id(A.X)
    }

    val a3: A = select(A.X, A.Y)
    // There's a room for improvement, but it's potentially hard to support because we complete `id` calls in a FULL mode independently
    val a4: A = select(id(A.X), id(A.X))

    val a5: A = when {
        true -> <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.X<!>
        else -> <!DEBUG_INFO_CSR_MIGHT_BE_USED!>A.Y<!>
    }

    select(A.X, A.Y)
    select(id(A.X), id(A.Y))

    val a6 = when {
        true -> A.X
        else -> A.Y
    }

    val a7: A = myRun {
        A.X
    }

    val a8: A = myRun {
        id(A.X)
    }

    val a9 = myRun {
        A.X
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
