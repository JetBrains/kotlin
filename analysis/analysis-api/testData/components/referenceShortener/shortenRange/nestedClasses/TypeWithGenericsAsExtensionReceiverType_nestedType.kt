package test

class Outer<T> {
    class Nested
}

fun <T> Outer<T>.test(p: <expr>Outer.Nested</expr>) {}