// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common

// FILE: common.kt

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun existingMethod()
}

// MODULE: m2-jvm()()(m1-common)

// FILE: jvm.kt

actual typealias Foo = FooImpl

// FILE: FooImpl.java

public class FooImpl {
    public void existingMethod() {}

    public void injectedMethod() {}
}
