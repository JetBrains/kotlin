// FILE: Test.java

class Test {
    static void foo(Comparable x) {}
}

// FILE: main.kt

fun main() {
    Test.foo(1)
}