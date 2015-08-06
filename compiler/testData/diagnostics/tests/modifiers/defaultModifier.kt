<!WRONG_MODIFIER_TARGET!>companion<!> class A {
    companion object {

    }
}

class B {
    companion object

    val c: Int = 1
}

class C {
    companion object A {

    }
}

class D {
    companion object A {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {
        }
    }
}

<!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object G {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object
}

<!WRONG_MODIFIER_TARGET!>companion<!> interface H {
    companion object
}

class J {
    companion object C {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object
    }
}

<!WRONG_MODIFIER_TARGET!>companion<!> enum class Enum {
    E1,
    E2;

    companion object
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun main() {

}

<!WRONG_MODIFIER_TARGET!>companion<!> var prop: Int = 1
    <!WRONG_MODIFIER_TARGET!>companion<!> get
    <!WRONG_MODIFIER_TARGET!>companion<!> set

class Z(<!WRONG_MODIFIER_TARGET!>companion<!> val c: Int)