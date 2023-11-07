// TYPE: kotlin/Boolean
// TYPE: kotlin/String
// TYPE: kotlin/Int
// TYPE: com.x.y.z/Foo.X.Y
// TYPE: com.x.y.z/Foo.A.B.C.D
// TYPE: com.x.y.z/P.Q.R.S
package com.x.y.z
class Foo {
    <expr>class Bar</expr>
    class X {
        class Y
    }

    class A {
        class B {
            class C {
                class D
            }
        }
    }
}

class P {
    class Q {
        class R {
            class S
        }
    }
}