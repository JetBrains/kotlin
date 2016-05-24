// PARAM_TYPES: A
// PARAM_TYPES: B
// PARAM_DESCRIPTOR: public final class A defined in root package
// PARAM_DESCRIPTOR: public final fun B.foo(): kotlin.Int defined in A
// SIBLING:
class A {
    val a = 1

    fun B.foo() = <selection>a + b</selection>
}

class B {
    val b = 1
}