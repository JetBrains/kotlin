package test

interface A {
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> val a: String
      get() = "AAAA!"
}

open class C {
    private val a: String = ""
}

class Subject : C(), A {
    val c = <!INVISIBLE_MEMBER!>a<!>
}