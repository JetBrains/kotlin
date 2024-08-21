// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
open class Base() {
    open fun fakeOverride() {}
}

expect class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual public class Foo extends Base {
    @kotlin.jvm.KotlinActual public Foo() {}
    @kotlin.jvm.KotlinActual public void foo() {}
    @kotlin.jvm.KotlinActual @Override void fakeOverride() {}
}
