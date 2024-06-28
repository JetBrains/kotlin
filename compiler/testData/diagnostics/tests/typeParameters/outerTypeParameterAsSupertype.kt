// FIR_IDENTICAL

package test

interface OuterParam

class A: OuterParam

class Outer<OuterParam> {

    class Nested: OuterParam {
        fun foo(): OuterParam = A()
    }
}

fun main() {
    val c: OuterParam = Outer.Nested().foo()
}