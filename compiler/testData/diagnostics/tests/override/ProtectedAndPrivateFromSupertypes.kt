package test

interface A {
    val a: String
}

interface B {
    val a: String
}

open class C {
    private val a: String = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Subject<!> : C(), A, B {
    val c = a
}