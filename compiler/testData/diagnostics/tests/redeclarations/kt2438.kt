// FIR_IDENTICAL
// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
//KT-2438 Prohibit inner classes with the same name

package kt2438

class B {
    class <!REDECLARATION!>C<!>
    class <!REDECLARATION!>C<!>

    class <!CONFLICTING_OVERLOADS, REDECLARATION!>D<!>
    class <!CONFLICTING_OVERLOADS, REDECLARATION!>D<!>
    <!CONFLICTING_OVERLOADS!>fun D()<!> {}
}



class A {
    class <!REDECLARATION!>B<!>

    companion object {
        class <!REDECLARATION!>B<!>
        class <!REDECLARATION!>B<!>
    }

    class <!REDECLARATION!>B<!>
}
