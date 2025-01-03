// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common

// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!>() {
    open val foo: Int = 1
}

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> : Base {}

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual typealias Foo = FooImpl

// FILE: FooImpl.java

public class FooImpl extends Base {
    @Override
    public int getFoo() {
        return 1;
    }
}
