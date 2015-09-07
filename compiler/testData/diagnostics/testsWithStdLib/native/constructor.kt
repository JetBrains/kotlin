class A {
    <!WRONG_MODIFIER_TARGET!>external<!> constructor() {}
    inner class B {
        <!WRONG_MODIFIER_TARGET!>external<!> constructor() {}
    }

    <!WRONG_MODIFIER_TARGET!>external<!> constructor(<!UNUSED_PARAMETER!>x<!>: Int)
}

class C <!WRONG_MODIFIER_TARGET!>external<!> constructor()