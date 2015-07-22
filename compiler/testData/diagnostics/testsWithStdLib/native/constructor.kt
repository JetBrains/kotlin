class A {
    <!WRONG_ANNOTATION_TARGET!>native<!> constructor() {}
    inner class B {
        <!WRONG_ANNOTATION_TARGET!>native<!> constructor() {}
    }

    <!WRONG_ANNOTATION_TARGET!>native<!> constructor(<!UNUSED_PARAMETER!>x<!>: Int)
}

class C <!WRONG_ANNOTATION_TARGET!>native<!> constructor()