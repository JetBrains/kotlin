<!ILLEGAL_MODIFIER!>default<!> class A {
    default object {

    }
}

class B {
    default object

    val c: Int = 1
}

class C {
    default object A {

    }
}

class D {
    default object A {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object {
        }
    }
}

<!ILLEGAL_MODIFIER!>default<!> object G {
    <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object
}

<!ILLEGAL_MODIFIER!>default<!> trait H {
    default object
}

class J {
    default object C {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object
    }
}

<!ILLEGAL_MODIFIER!>default<!> enum class Enum {
    E1
    E2

    default object
}

<!ILLEGAL_MODIFIER!>default<!> fun main() {

}

<!ILLEGAL_MODIFIER!>default<!> var prop: Int = 1
    <!ILLEGAL_MODIFIER!>default<!> get
    <!ILLEGAL_MODIFIER!>default<!> set

class Z(<!ILLEGAL_MODIFIER!>default<!> val c: Int)