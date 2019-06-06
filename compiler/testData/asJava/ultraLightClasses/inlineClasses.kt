inline class UInt(private val value: Int) { }

inline enum class Foo(val x: Int) {
    A(0), B(1);

    fun example() { }
}

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
