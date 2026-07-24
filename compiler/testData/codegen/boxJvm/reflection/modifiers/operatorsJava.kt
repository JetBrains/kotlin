// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// FILE: J.java
public class J {
    public int invoke(int x) { return x; }
    public static J invoke(String s) { return null; }

    @Override
    public boolean equals(Object o) { return this == o; }
    public boolean equals(J j) { return this == j; }

    public int get(int i) { return 0; }
    public int get() { return 0; }

    public void set(int i, Object v) {}
    public void set(int i) {}

    public boolean contains(Object o) { return false; }
    public Object iterator() { return null; }
    public Object next() { return null; }
    public boolean hasNext() { return false; }
    public J rangeTo(Object o) { return null; }
    public J rangeUntil(Object o) { return null; }
    public int compareTo(Object o) { return 0; }
    public J plus(Object o) { return null; }
    public J minus(Object o) { return null; }
    public J times(Object o) { return null; }
    public J div(Object o) { return null; }
    public J rem(Object o) { return null; }
    public J unaryPlus() { return null; }
    public J unaryMinus() { return null; }
    public J not() { return null; }
    public J inc() { return null; }
    public J dec() { return null; }
    public void plusAssign(Object o) {}
    public void minusAssign(Object o) {}
    public void timesAssign(Object o) {}
    public void divAssign(Object o) {}
    public void remAssign(Object o) {}
    public int component1() { return 0; }
    public int component5() { return 0; }

    public String getValue(Object o1, Object o2) { return null; }
    public void setValue(Object o1, Object o2, String o3) { }
}

// FILE: J2.java
public class J2 {
    public void compareTo(Object other) {}
    public int contains(Object o) { return 0; }
    public Object inc() { return null; }
    public Object dec() { return null; }
    public String getValue(Object o1, Object o2, Object o3) { return null; }
    public void setValue(Object o1, Object o2, String o3, Object o4) { }
}

// FILE: box.kt
import kotlin.reflect.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun box(): String {
    val invoke1: KFunction2<J, Int, Int> = J::invoke
    assertTrue(invoke1.isOperator)
    val invoke2: KFunction1<String, J?> = J::invoke
    assertFalse(invoke2.isOperator)

    val equals1: KFunction2<J, Any?, Boolean> = J::equals
    assertTrue(equals1.isOperator)
    val equals2: KFunction2<J, J, Boolean> = J::equals
    assertFalse(equals2.isOperator)

    val get1: KFunction2<J, Int, Int> = J::get
    assertTrue(get1.isOperator)
    val get2: KFunction1<J, Int> = J::get
    assertFalse(get2.isOperator)

    val set1: KFunction3<J, Int, Any?, Unit> = J::set
    assertTrue(set1.isOperator)
    val set2: KFunction2<J, Int, Unit> = J::set
    assertFalse(set2.isOperator)

    assertTrue(J::contains.isOperator)

    assertTrue(J::iterator.isOperator)

    assertTrue(J::next.isOperator)

    assertTrue(J::hasNext.isOperator)

    assertTrue(J::rangeTo.isOperator)
    assertTrue(J::rangeUntil.isOperator)

    assertTrue(J::compareTo.isOperator)

    assertTrue(J::plus.isOperator)
    assertTrue(J::minus.isOperator)
    assertTrue(J::times.isOperator)
    assertTrue(J::div.isOperator)
    assertTrue(J::rem.isOperator)

    assertTrue(J::unaryPlus.isOperator)
    assertTrue(J::unaryMinus.isOperator)
    assertTrue(J::not.isOperator)

    assertTrue(J::inc.isOperator)
    assertTrue(J::dec.isOperator)

    assertTrue(J::plusAssign.isOperator)
    assertTrue(J::minusAssign.isOperator)
    assertTrue(J::timesAssign.isOperator)
    assertTrue(J::divAssign.isOperator)
    assertTrue(J::remAssign.isOperator)

    assertTrue(J::component1.isOperator)
    assertTrue(J::component5.isOperator)

    assertTrue(J::getValue.isOperator)
    assertTrue(J::setValue.isOperator)

    assertFalse(J2::compareTo.isOperator)
    assertFalse(J2::contains.isOperator)
    assertFalse(J2::inc.isOperator)
    assertFalse(J2::dec.isOperator)

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // K1 doesn't support `LanguageFeature.ForbidGetSetValueWithTooManyParameters`
        assertTrue(J2::setValue.isOperator)
        assertTrue(J2::getValue.isOperator)
    } else {
        assertFalse(J2::setValue.isOperator)
        assertFalse(J2::getValue.isOperator)
    }

    return "OK"
}
