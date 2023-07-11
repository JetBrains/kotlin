// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Base() {
    open val foo: Int = 1
}

expect open class Foo : Base {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Foo = FooImpl

// FILE: Foo.java

public class FooImpl extends Base {
    @Override
    public int getFoo() {
        return 1;
    }
}
