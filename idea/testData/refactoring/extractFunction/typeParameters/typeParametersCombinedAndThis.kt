// PARAM_TYPES: A<T>
// PARAM_TYPES: V, Data
// PARAM_DESCRIPTOR: internal final class A<T : Data> where T : DataEx defined in root package
// PARAM_DESCRIPTOR: value-parameter val v: V defined in A.foo
open class Data(val x: Int)
interface DataEx

// SIBLING:
class A<T: Data>(val t: T) where T: DataEx {
    fun foo<V: Data>(v: V): Int {
        return <selection>t.x + v.x</selection>
    }
}