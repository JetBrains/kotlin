//KT-2438 Prohibit inner classes with the same name

package kt2438

class B {
    class <!REDECLARATION!>C<!>
    class <!REDECLARATION!>C<!>
}



class A {
    class <!REDECLARATION!>B<!>
    
    default object {
        class <!REDECLARATION!>B<!>
        class <!REDECLARATION!>B<!>
    }
    
    class <!REDECLARATION!>B<!>
}
