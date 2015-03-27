// PARAM_DESCRIPTOR: val a: foo.A defined in foo.bar
// PARAM_TYPES: T
trait T

// SIBLING:
fun foo(): T {
    class A: T

    fun bar(): T {
        val a = A()
        return <selection>a</selection>
    }
}