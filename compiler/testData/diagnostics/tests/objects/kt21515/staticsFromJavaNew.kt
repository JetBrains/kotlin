// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

// FILE: test/Java.java
package test;

public class Java {
    public static void method() { }
    public static int property = 42;
    public static class Classifier { }
    public static void syntheticSam(Runnable r) { }

    public static int getStaticSyntheticProperty() { return 42; }
    public static int setStaticSyntheticProperty(int x) { return 42; }

    public int getInstanceSyntheticProperty() { return 42; }
    public int setInstanceSyntheticProperty(int x) { return 42; }
}

// FILE: Kotlin.kt
package test

open class Base {
    companion object : Java() {

    }
}

class Derived : Base() {
    fun test(javaStaticInTypePosition: <!UNRESOLVED_REFERENCE!>Classifier<!>) {
        <!UNRESOLVED_REFERENCE!>method<!>()
        <!UNRESOLVED_REFERENCE!>property<!>
        <!UNRESOLVED_REFERENCE!>Classifier<!>()
        <!UNRESOLVED_REFERENCE!>syntheticSam<!> { }

        // Instance members shouldn't be affected, but we check them, just in case
        val y = instanceSyntheticProperty
        instanceSyntheticProperty = 43

        // Note that statics actually aren't converted into synthetic property in Kotlin
        val x = <!UNRESOLVED_REFERENCE!>syntheticProperty<!>
        <!UNRESOLVED_REFERENCE!>syntheticProperty<!> = 42
    }

    class JavaStaticInSupertypeList : <!UNRESOLVED_REFERENCE!>Classifier<!>() {

    }
}