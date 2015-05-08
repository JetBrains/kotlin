<!ILLEGAL_MODIFIER!>companion<!> class A {
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
        <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object {
        }
    }
}

<!ILLEGAL_MODIFIER!>companion<!> object G {
    <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object
}

<!ILLEGAL_MODIFIER!>companion<!> interface H {
    companion object
}

class J {
    companion object C {
        <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object
    }
}

<!ILLEGAL_MODIFIER!>companion<!> enum class Enum {
    E1,
    E2;

    companion object
}

<!ILLEGAL_MODIFIER!>companion<!> fun main() {

}

<!ILLEGAL_MODIFIER!>companion<!> var prop: Int = 1
    <!ILLEGAL_MODIFIER!>companion<!> get
    <!ILLEGAL_MODIFIER!>companion<!> set

class Z(<!ILLEGAL_MODIFIER!>companion<!> val c: Int)