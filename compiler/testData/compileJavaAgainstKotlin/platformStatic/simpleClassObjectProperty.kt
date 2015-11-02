package test

class A {
    companion object {
        @JvmStatic val b: String = "OK"

        var A.c: String
            @JvmStatic get() = "OK"
            @JvmStatic set(t: String) {}
    }
}

fun main(args: Array<String>) {
    A.b
    with(A) {
        A().c
        A().c = "123"
    }
}