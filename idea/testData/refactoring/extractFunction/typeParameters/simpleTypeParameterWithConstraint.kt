open class Data(val x: Int)
trait DataEx

class Pair<A, B>(val a: A, val b: B)

// SIBLING:
fun foo<V: Data>(v: V): Pair<Int, V> where V: DataEx {
    return <selection>Pair(v.x + 10, v)</selection>
}