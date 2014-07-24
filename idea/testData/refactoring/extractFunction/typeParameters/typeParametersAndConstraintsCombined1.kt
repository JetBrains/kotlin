// PARAM_TYPES: A<T>
// PARAM_TYPES: A.B<U>
// PARAM_TYPES: V, Data
// PARAM_DESCRIPTOR: internal final class A<T : DataEx> where T : Data defined in root package
// PARAM_DESCRIPTOR: internal final inner class B<U : DataExEx> where U : Data defined in A
// PARAM_DESCRIPTOR: value-parameter val v: V defined in A.B.foo
open class Data(val x: Int)
trait DataEx
trait DataExEx

// SIBLING:
class A<T: Data>(val t: T) where T: DataEx {
    inner class B<U: Data>(val u: U) where U: DataExEx {
        fun foo<V: Data>(v: V): Int where V: DataEx {
            return <selection>t.x + u.x + v.x</selection>
        }
    }
}