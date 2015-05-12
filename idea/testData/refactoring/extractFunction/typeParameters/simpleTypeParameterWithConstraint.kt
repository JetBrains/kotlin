// PARAM_TYPES: V
// PARAM_DESCRIPTOR: value-parameter val v: V defined in foo
open class Data(val x: Int)
interface DataEx

class Pair<A, B>(val a: A, val b: B)

// SIBLING:
fun foo<V: Data>(v: V): Pair<Int, V> where V: DataEx {
    return <selection>Pair(v.x + 10, v)</selection>
}