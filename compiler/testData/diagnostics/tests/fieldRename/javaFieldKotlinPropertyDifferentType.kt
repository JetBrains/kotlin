// ISSUE: KT-57905

// FILE: Base.java
public class Base {
    String value = null;
    String extension = null;
}

// FILE: Main.kt
class Derived: Base() {
    val value: Int = 42
    val something: String = <!BASE_CLASS_FIELD_WITH_DIFFERENT_SIGNATURE_THAN_DERIVED_CLASS_PROPERTY!>value<!>

    val String.extension: Int get() = 42
    fun String.foo() {
        // K1 & K2 work in the same way here (resolve to an extension property)
        val something: String = <!TYPE_MISMATCH!>extension<!>
    }
}
