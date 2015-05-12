interface A

fun foo(): A {
    return <caret>object: A {

    }
}