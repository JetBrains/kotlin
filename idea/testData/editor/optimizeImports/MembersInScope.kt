import Outer.Nested
import Outer.ClassObjectNested
import Outer.C.Inner
import Outer.C.Inner2

class Outer {
    class Nested

    class C {
        fun foo(p1: Nested, p2: ClassObjectNested, p3: Inner) { }

        inner class Inner
        inner class Inner2
    }

    companion object {
        class ClassObjectNested
    }

    fun f(i: Inner2){}
}