// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect sealed class Foo {
    class Bar : Foo
    class Baz : Foo
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
