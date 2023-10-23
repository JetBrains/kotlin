package test

class Outer<T> {
    inner class Inner
}

fun <T> Outer<T>.test(p: <expr>Outer<T>.Inner</expr>) {}