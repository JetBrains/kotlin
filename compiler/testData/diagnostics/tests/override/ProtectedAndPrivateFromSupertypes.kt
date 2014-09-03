package test

trait A {
    protected val a: String
}

trait B {
    protected val a: String
}

open class C {
    private val a: String = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Subject<!> : C(), A, B {
    val c = a
}