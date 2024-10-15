// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect sealed class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!> {
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Bar<!> : <!CLASS_INHERITS_JAVA_SEALED_CLASS{JVM}!>Foo<!>
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Baz<!> : <!CLASS_INHERITS_JAVA_SEALED_CLASS{JVM}!>Foo<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public sealed class Foo permits Foo.Bar, Foo.Baz {
    @kotlin.annotations.jvm.KotlinActual
    public static final class Bar extends Foo { }
    @kotlin.annotations.jvm.KotlinActual
    public static final class Baz extends Foo { }
}
