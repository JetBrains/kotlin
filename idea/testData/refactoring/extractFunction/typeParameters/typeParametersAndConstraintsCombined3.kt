// PARAM_TYPES: V, Data
open class Data(val x: Int)
trait DataEx
trait DataExEx

class A<T: Data>(val t: T) where T: DataEx {
    inner class B<U: Data>(val u: U) where U: DataExEx {
        // SIBLING:
        fun foo<V: Data>(v: V): Int where V: DataEx {
            return <selection>t.x + u.x + v.x</selection>
        }
    }
}