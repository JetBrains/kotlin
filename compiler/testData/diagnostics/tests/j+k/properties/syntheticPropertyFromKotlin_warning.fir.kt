// LANGUAGE: -ForbidSyntheticPropertiesWithoutBaseJavaGetter

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
    x.<!SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN("fun getFoo(): String; getFoo")!>foo<!>
    x.<!SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN("fun setFoo(value: String): Unit; setFoo")!>foo<!> = "a"
}

fun test_2(x: Impl) {
    x.<!SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN("fun getBar(): String; getBar")!>bar<!>
    x.<!SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN("fun setBar(value: String!): Unit; setBar")!>bar<!> = "a"
}
