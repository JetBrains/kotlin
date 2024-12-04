// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -DontCreateSyntheticPropertiesWithoutBaseJavaGetter +ForbidSyntheticPropertiesWithoutBaseJavaGetter

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
    x.<!FUNCTION_CALL_EXPECTED!>foo<!>
    x.<!FUNCTION_CALL_EXPECTED!>foo<!> = "a"
}

fun test_2(x: Impl) {
    x.<!FUNCTION_CALL_EXPECTED!>bar<!>
    x.<!FUNCTION_CALL_EXPECTED!>bar<!> = "a"
}
