// PARAM_TYPES: Data<T>
class Data<T>(val t: Int)

// SIBLING:
class A<T> {
    fun foo(d: Data<T>): Int {
        return <selection>d.t + 1</selection>
    }
}