open class Data(val x: Int)
trait DataEx

// NEXT_SIBLING:
class A<T: Data>(val t: T) where T: DataEx {
    fun foo<V: Data>(v: V): Int {
        return <selection>t.x + v.x</selection>
    }
}