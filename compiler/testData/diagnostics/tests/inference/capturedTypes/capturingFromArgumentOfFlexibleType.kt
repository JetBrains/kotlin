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
    <!DEBUG_INFO_EXPRESSION_TYPE("(Foo<out (kotlin.Number..kotlin.Number?)>..Foo<out (kotlin.Number..kotlin.Number?)>?)")!>Test.getFoo()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(Foo<out (kotlin.Number..kotlin.Number?)>..Foo<out (kotlin.Number..kotlin.Number?)>?)")!>id(Test.getFoo())<!>
}
