// PARAM_TYPES: Data<T>
// PARAM_DESCRIPTOR: value-parameter val d: Data<T> defined in A.foo
class Data<T>(val t: Int)

// SIBLING:
class A<T> {
    fun foo(d: Data<T>): Int {
        return <selection>d.t + 1</selection>
    }
}