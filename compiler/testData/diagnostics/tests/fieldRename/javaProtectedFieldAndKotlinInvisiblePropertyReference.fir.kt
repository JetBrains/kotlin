// RUN_PIPELINE_TILL: FRONTEND
// FILE: base/BaseJava.java

package base;

public class BaseJava {
    protected String a = "TARGET";

    String b = "";
}

// FILE: base/DerivedJava.java

package base;

class DerivedJava extends BaseKotlin {
    protected String a = "";
}

// FILE: Base.kt

package base

abstract class BaseKotlin

open class Intermediate : BaseJava() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> = ""
}

class Derived : Intermediate() {
    fun foo() = this::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> // Same package
}

private class DerivedFromDerivedJava : DerivedJava() {
    fun foo() = this::a // Property class is a subclass of the field class
}

// FILE: Derived.kt

package derived

import base.BaseJava

open class Intermediate : BaseJava() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> = ""

    private val b = ""
}

open class IntermediateWithoutField : BaseJava() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> get() = ""
}

open class IntermediatePublic : BaseJava() {
    val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> = ""
}

class Derived : Intermediate() {
    // This should be the first erroneous place (only in K2)
    fun foo() = this::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>

    fun bar() = <!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!> // Non-reference

    fun baz() = this::<!INVISIBLE_REFERENCE!>b<!> // Non-protected
}

typealias Alias = Intermediate

class DerivedAlias : Alias() {
    // This should be the second erroneous place (only in K2)
    fun foo() = this::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>
}

fun local() {
    open class LocalIntermediate : BaseJava() {
        private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!> = ""
    }

    class LocalDerived : LocalIntermediate() {
        // This should be the third and the last erroneous place (only in K2)
        fun foo() = this::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>a<!>
    }
}

class DerivedWithoutBackingField : IntermediateWithoutField() {
    fun foo() = this::a // No shadowing backing field
}

class DerivedPublic : IntermediatePublic() {
    fun foo() = this::a // Visible property
}

class DirectlyDerived : BaseJava() {
    fun foo() = this::a // No property at all
}

fun test(d: Derived) {
    d::<!INVISIBLE_REFERENCE!>a<!> // Field is also invisible
}
