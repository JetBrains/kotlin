// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class KotlinModifiers {
    operator fun plus(i: Int)
    inline fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>()
    suspend fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>bar<!>()
    infix fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>qux<!>(i: Int)
}

expect class JavaModifiers {
    fun foo()
    fun bar()
    fun qux()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: KotlinModifiers.java
@kotlin.annotations.jvm.KotlinActual
public class KotlinModifiers {
    @kotlin.annotations.jvm.KotlinActual
    public void plus(int i){}
    @kotlin.annotations.jvm.KotlinActual
    public void foo(){}
    @kotlin.annotations.jvm.KotlinActual
    public void bar(){}
    @kotlin.annotations.jvm.KotlinActual
    public void qux(int i){}
}

// FILE: JavaModifiers.java
@kotlin.annotations.jvm.KotlinActual
public class JavaModifiers {
    @kotlin.annotations.jvm.KotlinActual
    public native void foo();
    @kotlin.annotations.jvm.KotlinActual
    public synchronized void bar(){}
    @kotlin.annotations.jvm.KotlinActual
    public default void qux(){};
}
