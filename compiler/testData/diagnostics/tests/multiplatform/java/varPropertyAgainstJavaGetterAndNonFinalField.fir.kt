// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    var foo: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface I {
    val foo: Int
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = JavaFoo

// FILE: JavaFoo.java
public class JavaFoo implements I {
    public int foo;

    @Override
    public int getFoo() {
        return 0;
    }
}
