// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// FILE: test1.kt
package test1

interface I1 {
    context(x: Int) fun foo()
}

interface I2 {
    context(y: Int) fun foo()
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface I3<!> : I1, I2

class C1 : I1 {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>xx<!>: Int) override fun foo() {}
}

// FILE: test2.kt
package test2

interface I1 {
    context(x: Int) fun foo()
}

interface I2 {
    context(_: Int) fun foo()
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface I3<!> : I1, I2

class C1 : I1 {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>xx<!>: Int) override fun foo() {}
}

// FILE: test3.kt
package test3

interface I1 {
    context(x: Int) fun foo()
}

interface I2 {
    context(_: Int) fun foo()
}

<!DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES!>interface I3<!> : I1, I2

class C1 : I1 {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>_<!>: Int) override fun foo() {}
}
