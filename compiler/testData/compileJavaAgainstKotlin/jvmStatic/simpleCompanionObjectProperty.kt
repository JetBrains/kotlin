// FILE: simpleCompanionObjectProperty.java
package test;

class Test {

    public static void main(String[] args) {
        A.getB();
        A.getC(new A());
    }

}

// FILE: simpleCompanionObjectProperty.kt
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
