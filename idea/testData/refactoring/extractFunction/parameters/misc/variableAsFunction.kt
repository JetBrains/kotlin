// PARAM_TYPES: A
class A {
    fun invoke() = 20
}
// SIBLING:
fun testProp() {
    val foo = A()
    <selection>foo()</selection>
}