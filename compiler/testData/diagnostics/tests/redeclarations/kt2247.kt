//KT-2247 Report name clashes between inner classes and members of class object

package kt2247

class B {
    class object {
        class <!REDECLARATION!>Y<!> {
        }
    }

    class <!REDECLARATION!>Y<!> {
    }

}
