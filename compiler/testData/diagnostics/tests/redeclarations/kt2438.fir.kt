// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
//KT-2438 Prohibit inner classes with the same name

package kt2438

class B {
    <!REDECLARATION!>class C<!>
    <!REDECLARATION!>class C<!>
}



class A {
    <!REDECLARATION!>class B<!>
    
    companion object {
        <!REDECLARATION!>class B<!>
        <!REDECLARATION!>class B<!>
    }
    
    <!REDECLARATION!>class B<!>
}
