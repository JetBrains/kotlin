// PARAM_TYPES: V, Data
// PARAM_DESCRIPTOR: value-parameter val v: V defined in A.B.foo
open class Data(val x: Int)

class A<T: Data>(val t: T) {
    inner class B<U: Data>(val u: U) {
        // SIBLING:
        fun foo<V: Data>(v: V): Int {
            return <selection>t.x + u.x + v.x</selection>
        }
    }
}