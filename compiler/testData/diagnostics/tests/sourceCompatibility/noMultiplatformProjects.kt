// RUN_PIPELINE_TILL: FRONTEND
<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> fun foo1()
<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> val bar1 = <!EXPECTED_PROPERTY_INITIALIZER!>42<!>
<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> class ImplicitExpect {
    fun foo()
    val x: Int
    class Inner
}

<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> class ExplicitExpect {
    <!WRONG_MODIFIER_TARGET!>expect<!> fun explicitFoo()
    <!WRONG_MODIFIER_TARGET!>expect<!> val explicitX: Int
    <!NOT_A_MULTIPLATFORM_COMPILATION, WRONG_MODIFIER_TARGET!>expect<!> class ExplicitInner
}

<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> fun foo2() = 42
<!MUST_BE_INITIALIZED!><!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> val bar2: Int<!>
<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> interface Baz2

<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> class ImplicitExpect {
    <!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> fun foo() {
    }
    <!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> val x: Int = 0
    <!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> class Inner
}