// FILE: A.java

public class A {
    public static void test() {}
}

// FILE: test.kt

enum class E { EN }

fun test() {
    A()::test
    E.EN::valueOf
}
