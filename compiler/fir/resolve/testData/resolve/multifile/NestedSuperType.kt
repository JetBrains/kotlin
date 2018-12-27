package a

import b.B

class A : B() {
    class NestedInA1 : NestedInB()
    class NestedInA2 : NestedInC()
}