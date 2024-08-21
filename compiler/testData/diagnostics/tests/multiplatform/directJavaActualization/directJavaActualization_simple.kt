// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
open class Base() {
    open fun fakeOverrideInExpect() {}
}

expect open class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!>() : Base {
    fun foo()
    open fun fakeOverrideInActual()

    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Nested<!>
    inner class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Inner<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual public class Foo extends Base implements JavaBase {
    @kotlin.jvm.KotlinActual public Foo() {}
    @kotlin.jvm.KotlinActual public void foo() {}

    @Override
    public void fakeOverrideInExpect() {}

    public void additionalMember() {}

    @kotlin.jvm.KotlinActual public static class Nested {}
    @kotlin.jvm.KotlinActual public class Inner {}
}

// FILE: JavaBase.java
public interface JavaBase {
    default void fakeOverrideInActual() {}
}
