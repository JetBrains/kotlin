// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST

// FILE: Test.java
class Test {
    static Foo<? extends Number> getFoo() {
        return null;
    }
}

// FILE: main.kt
class Foo<T>

fun <T> id(x: T) = null as T

fun test() {
    Test.getFoo()
    id(Test.getFoo())
}
