// "Change parameter 'a' type of function 'times' to 'String'" "true"
interface A {
    operator fun times(a: A): A
}

fun foo(a: A): A = a * <caret>""