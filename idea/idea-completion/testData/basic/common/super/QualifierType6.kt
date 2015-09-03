package p

open class A

interface I {
    interface Nested1
    interface Nested2
}

class B : A(), I.Nested1 {
    fun foo() {
        super<I.<caret>
    }
}

// EXIST: Nested1
// NOTHING_ELSE