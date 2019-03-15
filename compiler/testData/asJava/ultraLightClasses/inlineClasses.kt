/** should load cls */
inline class UInt(private val value: Int) { }

/** should load cls */
inline enum class Foo(val x: Int) {
    A(0), B(1);

    fun example() { }
}

/** should load cls */
inline class InlinedDelegate<T>(var node: T) {
    operator fun setValue(thisRef: A, property: KProperty<*>, value: T) {
        if (node !== value) {
            thisRef.notify(node, value)
        }
        node = value
    }

    operator fun getValue(thisRef: A, property: KProperty<*>): T {
        return node
    }
}
