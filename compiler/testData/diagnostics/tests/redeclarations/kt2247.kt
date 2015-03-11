//KT-2247 Report name clashes between inner classes and members of default object

package kt2247

class B {
    default object {
        class <!REDECLARATION!>Y<!> {
        }
    }

    class <!REDECLARATION!>Y<!> {
    }

}
