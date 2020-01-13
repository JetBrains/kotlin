// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: JTest.java
public class JTest {
    public static String foo() { return ""; }
    public static class Nested {}
}

// FILE: JDerived.java
public class JDerived extends JTest {
}

// FILE: test.kt
class KTest {
    class Nested
    inner class Inner
}

interface ITest {
    class Nested
}

typealias JT = JTest
typealias JD = JDerived
typealias KT = KTest
typealias IT = ITest

// Referencing Java class static members via type alias should be ok
val testFoo: String = JT.foo()
val seeAlsoFoo: String = JTest.foo()
// Referencing base Java class static members via type alias for derived Java class should be ok
val testDerivedFoo: String = JD.foo()
val seeAlsoDerivedFoo: String = JDerived.foo()

// Referencing nested classes via type alias should be prohibited
// (in type position and in expression position)
val testNested1: JT.Nested = JT.<!UNRESOLVED_REFERENCE!>Nested<!>()
val testNested2: KT.Nested = KT.Nested()
val testNested3: IT.Nested = IT.Nested()
val testInner1: JT.Inner = JT.<!UNRESOLVED_REFERENCE!>Inner<!>()
val testInner2: KT.Inner = KT.<!UNRESOLVED_REFERENCE!>Inner<!>()
fun testNestedAsTypeArgument1(x: List<JT.Nested>) {}
fun testNestedAsTypeArgument2(x: List<KT.Nested>) {}
fun testNestedAsTypeArgument3(x: List<IT.Nested>) {}
fun testInnerAsTypeArgument1(x: List<JT.Inner>) {}
fun testInnerAsTypeArgument2(x: List<KT.Inner>) {}

