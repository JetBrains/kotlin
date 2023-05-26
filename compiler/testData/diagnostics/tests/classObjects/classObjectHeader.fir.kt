package test

open class ToResolve<SomeClass>(f : (Int) -> Int)
fun testFun(a : Int) = 12

class TestSome<P> {
    companion object : ToResolve<<!UNRESOLVED_REFERENCE!>P<!>>(<!ARGUMENT_TYPE_MISMATCH!>{testFun(it)}<!>) {
    }
}
