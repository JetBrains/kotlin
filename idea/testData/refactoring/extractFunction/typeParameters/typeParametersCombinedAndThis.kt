// PARAM_TYPES: A<T>
// PARAM_TYPES: V, Data
open class Data(val x: Int)
trait DataEx

// SIBLING:
class A<T: Data>(val t: T) where T: DataEx {
    fun foo<V: Data>(v: V): Int {
        return <selection>t.x + v.x</selection>
    }
}