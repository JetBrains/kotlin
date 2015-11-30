// "Change 'A.not' function return type to 'A'" "true"
interface A {
    operator fun not(): String
    operator fun times(a: A): A
}

fun foo(a: A): A = a * <caret>!(if (true) a else a)