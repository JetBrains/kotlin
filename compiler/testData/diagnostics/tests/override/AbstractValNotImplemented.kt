// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
abstract class A {
    abstract val i: Int
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B<!>() : A() {
}
