// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect open class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!> {
    internal fun bar()
    protected fun foo()
    fun baz()
    public fun qux()
}

internal expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Bar<!>

expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Baz<!>

public expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Qux<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo {
    @kotlin.annotations.jvm.KotlinActual
    public void bar(){}
    @kotlin.annotations.jvm.KotlinActual
    protected void foo(){}
    @kotlin.annotations.jvm.KotlinActual
    public void baz(){}
    @kotlin.annotations.jvm.KotlinActual
    public void qux(){}
}

// FILE: Bar.java
@kotlin.annotations.jvm.KotlinActual
public class Bar { }

// FILE: Baz.java
@kotlin.annotations.jvm.KotlinActual
class Baz { }

// FILE: Qux.java
@kotlin.annotations.jvm.KotlinActual
public class Qux { }