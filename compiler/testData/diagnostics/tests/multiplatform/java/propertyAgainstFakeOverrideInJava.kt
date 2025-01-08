// FIR_IDENTICAL
// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    val foo: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface I {
    val foo: Int
        get() = 1
}

actual typealias Foo = JavaFoo

// FILE: JavaFoo.java
public class JavaFoo implements I {
}
