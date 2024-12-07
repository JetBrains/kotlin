// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57905

// FILE: Base.java
public class Base {
    String value = null;
    String extension = null;
}

// FILE: Main.kt
class Derived: Base() {
    val <!PROPERTY_HIDES_JAVA_FIELD!>value<!>: Int = 42
    val something: String = <!INITIALIZER_TYPE_MISMATCH!>value<!>

    val String.extension: Int get() = 42
    fun String.foo() {
        // K1 & K2 work in the same way here (resolve to an extension property)
        val something: String = <!INITIALIZER_TYPE_MISMATCH!>extension<!>
    }
}
