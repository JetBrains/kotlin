class C {
    default object {
        fun f1(): C = C()
        fun f2(): C = C()
        fun f3(): C? = C()
    }
}

fun foo(c: C){}
fun foo(c: C?, i: Int){}

fun foo() {
    foo(<caret>
}

// ELEMENT: f1
