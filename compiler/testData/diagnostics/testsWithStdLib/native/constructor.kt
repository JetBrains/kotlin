class A {
    <!WRONG_ANNOTATION_TARGET!>external<!> constructor() {}
    inner class B {
        <!WRONG_ANNOTATION_TARGET!>external<!> constructor() {}
    }

    <!WRONG_ANNOTATION_TARGET!>external<!> constructor(<!UNUSED_PARAMETER!>x<!>: Int)
}

class C <!WRONG_ANNOTATION_TARGET!>external<!> constructor()