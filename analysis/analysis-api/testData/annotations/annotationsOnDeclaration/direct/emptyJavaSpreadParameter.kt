// FILE: A.java
public @interface A {
    String[] value() default {"foo", "bar"};
}

// FILE: Test.kt
annotation class B(vararg val a: A)

@B(A(), A(*[]))
class <caret>Foo