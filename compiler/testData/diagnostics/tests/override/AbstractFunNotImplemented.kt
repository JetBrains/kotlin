// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
abstract class A {
    abstract fun foo(): Int
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B<!>() : A() {
}