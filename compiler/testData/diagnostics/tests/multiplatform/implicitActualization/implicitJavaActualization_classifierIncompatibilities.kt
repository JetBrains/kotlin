// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
@file:OptIn(ExperimentalMultiplatform::class)

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

interface I

@ImplicitlyActualizedByJvmDeclaration expect class A
@ImplicitlyActualizedByJvmDeclaration expect value class B(val x: Int)
@ImplicitlyActualizedByJvmDeclaration expect fun interface C1 { fun foo() }
@ImplicitlyActualizedByJvmDeclaration expect fun interface C2 { fun foo() }
@ImplicitlyActualizedByJvmDeclaration expect class D1 : I
@ImplicitlyActualizedByJvmDeclaration expect class D2 : I
@ImplicitlyActualizedByJvmDeclaration expect enum class E1 { ONE, TWO }
@ImplicitlyActualizedByJvmDeclaration expect enum class E2 { ONE, TWO }
@ImplicitlyActualizedByJvmDeclaration expect class Outer {
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F1<!>
    inner class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F2<!>
    inner class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F3<!>
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F4<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: A.java
public interface A {}
// FILE: B.java
public class B {}
// FILE: C1.java
public interface C1 { public void foo(); }
// FILE: C2.java
public interface C2 { public void foo(); public void bar(); }
// FILE: D1.java
public class D1 implements I {}
// FILE: D2.java
public class D2 {}
// FILE: E1.java
public enum E1 { ONE, TWO }
// FILE: E2.java
public enum E2 { ONE }
// FILE: Outer.java
public class Outer {
    public static class F1 {}
    public class F2 {}
    public static class F3 {}
    public class F4 {}
}
