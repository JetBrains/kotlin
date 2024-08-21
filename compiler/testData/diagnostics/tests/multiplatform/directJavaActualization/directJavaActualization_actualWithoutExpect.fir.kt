// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
open class Base() {
    open fun fakeOverride() {}
}

<!JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT{JVM}, JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT{JVM}, JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT{JVM}!>expect class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual public class Foo extends Base {
    @kotlin.jvm.KotlinActual public Foo() {}
    @kotlin.jvm.KotlinActual public void foo() {}
    @kotlin.jvm.KotlinActual @Override void fakeOverride() {}
}
