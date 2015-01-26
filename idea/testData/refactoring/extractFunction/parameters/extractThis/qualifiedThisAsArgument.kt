// PARAM_TYPES: A
// PARAM_TYPES: B
// PARAM_DESCRIPTOR: internal final class A defined in root package
// PARAM_DESCRIPTOR: internal final fun B.foo(): Int defined in A
// SIBLING:
class A {
    val a = 1

    fun B.foo() = <selection>a + b</selection>
}

class B {
    val b = 1
}