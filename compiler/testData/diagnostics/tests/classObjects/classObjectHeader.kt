package test

open class ToResolve<SomeClass>(<!UNUSED_PARAMETER!>f<!> : (Int) -> Int)
fun testFun(<!UNUSED_PARAMETER!>a<!> : Int) = 12

class TestSome<P> {
    default object : ToResolve<<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>P<!>>({testFun(it)}) {
    }
}