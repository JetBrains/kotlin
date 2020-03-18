// FILE: C.kt

package c

open class C {
    open class NestedInC
}

// FILE: B.kt

package b

import c.C

open class B : C() {
    open class NestedInB : NestedInC()
}

// FILE: A.kt

package a

import b.B

class A : B() {
    class NestedInA1 : NestedInB()
    class NestedInA2 : NestedInC()
}