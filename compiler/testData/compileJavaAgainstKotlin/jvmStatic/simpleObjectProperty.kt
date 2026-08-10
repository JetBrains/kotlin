// FILE: simpleObjectProperty.java
package test;

class Test {

    public static void main(String[] args) {
        A.getB();
        A.getC(A.INSTANCE);
    }

}

// FILE: simpleObjectProperty.kt
package test

object A {
    @JvmStatic val b: String = "OK"

    var A.c: String
        @JvmStatic get() = "OK"
        @JvmStatic set(t: String) {}

}

fun main(args: Array<String>) {
    A.b
    with(A) {
        A.c
        A.c = "123"
    }
}
