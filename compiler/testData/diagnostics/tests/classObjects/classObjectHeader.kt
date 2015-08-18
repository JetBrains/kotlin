package test

open class ToResolve<SomeClass>(<!UNUSED_PARAMETER!>f<!> : (Int) -> Int)
fun testFun(<!UNUSED_PARAMETER!>a<!> : Int) = 12

class TestSome<P> {
    companion object : ToResolve<<!UNRESOLVED_REFERENCE!>P<!>>({testFun(it)}) {
    }
}