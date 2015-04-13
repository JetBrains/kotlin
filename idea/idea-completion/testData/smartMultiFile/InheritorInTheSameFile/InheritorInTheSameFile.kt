open class A : T

fun foo(): T {
    return <caret>
}

class B : T

// EXIST: A
// EXIST: B
// EXIST: C
