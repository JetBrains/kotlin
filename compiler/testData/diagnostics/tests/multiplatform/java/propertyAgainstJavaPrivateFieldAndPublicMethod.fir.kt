// MODULE: m1-common
// FILE: common.kt
interface I {
    val foo: Int
}

expect class Foo : I {
    override val foo: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = JavaFoo

// FILE: JavaFoo.java
public class JavaFoo implements I {
    private final int foo = 1;

    @Override
    public int getFoo() {
        return 0;
    }
}
