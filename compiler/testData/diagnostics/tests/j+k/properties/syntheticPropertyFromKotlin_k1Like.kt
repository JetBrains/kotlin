// FIR_IDENTICAL
// LANGUAGE: +ForbidSyntheticPropertiesWithoutBaseJavaGetter

// FILE: Base.kt
open class Base {
    fun getFoo(): String = ""
    fun setFoo(value: String) {}

    fun getBar(): String = ""
}

// FILE: Derived.java
public class Derived extends Base {
    public void setBar(String value) {}
}

// FILE: main.kt
class Impl : Derived()

fun test_1(x: Impl) {
    x.<!UNRESOLVED_REFERENCE!>foo<!>
    x.<!UNRESOLVED_REFERENCE!>foo<!> = "a"
}

fun test_2(x: Impl) {
    x.<!UNRESOLVED_REFERENCE!>bar<!>
    x.<!UNRESOLVED_REFERENCE!>bar<!> = "a"
}
