// FIR_IDENTICAL

class Y
class Z

class HashMap<K, V>(
    private val defaultValue: V
) {
    operator fun get(key: K): V = defaultValue
    operator fun set(key: K, value: V) {}
}

private operator fun Any.plusAssign(p: Any) { }
private operator fun Any.plus(p: Any): Any = Any()

class Base {
    private val x = HashMap<Y, Z>(Z())

    fun foo(): Z {
        val y = Y()
        val z = Z()
        x[y] += z
        return x[y]
    }
}