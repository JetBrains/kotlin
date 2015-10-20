class Outer {
    class Nested {
        class NestedNested
    }
    
    inner class Inner {
        inner class InnerInner
    }
}

fun f1() = Outer()
fun f2() = Outer.Nested()
fun f3() = Outer.Nested.NestedNested()
fun f4() = Outer.<!NO_COMPANION_OBJECT, FUNCTION_EXPECTED!>Inner<!>()
fun f5() = Outer.Inner.<!NO_COMPANION_OBJECT, FUNCTION_EXPECTED!>InnerInner<!>()
fun f6() = Outer().Inner()
fun f7() = Outer().Inner().InnerInner()