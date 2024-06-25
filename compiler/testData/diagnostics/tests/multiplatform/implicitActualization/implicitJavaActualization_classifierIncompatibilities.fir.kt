// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
@file:OptIn(ExperimentalMultiplatform::class)

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

interface I

<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect class <!CLASSIFIER_REDECLARATION!>A<!><!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect value class <!CLASSIFIER_REDECLARATION!>B<!>(val x: Int)<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect fun interface <!CLASSIFIER_REDECLARATION!>C1<!> { fun foo() }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect fun interface <!CLASSIFIER_REDECLARATION!>C2<!> { fun foo() }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect class <!CLASSIFIER_REDECLARATION!>D1<!> : I<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect class <!CLASSIFIER_REDECLARATION!>D2<!> : I<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect enum class <!CLASSIFIER_REDECLARATION!>E1<!> { ONE, TWO }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect enum class <!CLASSIFIER_REDECLARATION!>E2<!> { ONE, TWO }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>@ImplicitlyActualizedByJvmDeclaration expect class <!CLASSIFIER_REDECLARATION!>Outer<!> {
    class <!CLASSIFIER_REDECLARATION!>F1<!>
    inner class <!CLASSIFIER_REDECLARATION!>F2<!>
    inner class <!CLASSIFIER_REDECLARATION!>F3<!>
    class <!CLASSIFIER_REDECLARATION!>F4<!>
}<!>

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
