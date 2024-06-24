// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

@kotlin.jvm.KotlinActual public class Foo {
    @kotlin.jvm.KotlinActual public Foo() {}
    @kotlin.jvm.KotlinActual public void foo() {
    }
}
