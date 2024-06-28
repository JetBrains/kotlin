// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    var foo: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias Foo = JavaFoo

// FILE: JavaFoo.java
public class JavaFoo {
    public int foo = 0;
}
