// TYPE: kotlin/Boolean
// TYPE: kotlin/String
// TYPE: kotlin/Int
// TYPE: com.x.y.z/Foo.A.B.C.D
// TYPE: com.x.y.z/A.B.C.D
package com.x.y.z
class Foo {
    <expr>class Bar</expr>
    class A {
        class B {
            class C {
                class D
            }
        }
    }
}

class A {  // If we shorten the package from the fully-qualified reference to this class, it will have a name conflict with the above 'A'.
    class B {
        class C {
            class D
        }
    }
}