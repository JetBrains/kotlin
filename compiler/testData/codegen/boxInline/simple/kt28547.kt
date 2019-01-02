// FILE: 1.kt
package test

class C {
    var inserting: Boolean = false
    fun nextSlot(): Any? = null
    fun startNode(key: Any?) {}
    fun endNode() {}
    fun emitNode(node: Any?) {}
    fun useNode(): Any? = null
    fun skipValue() {}
    fun updateValue(value: Any?) {}
}

class B<T>(val composer: C, val node: T) {
    inline fun <V> bar(value: V, block: T.(V) -> Unit) = with(composer) {
        if (inserting || nextSlot() != value) {
            updateValue(value)
            node.block(value)
        } else skipValue()
    }
}

class A(val composer: C) {
    inline fun <T> foo(key: Any, ctor: () -> T, update: B<T>.() -> Unit) = with(composer) {
        startNode(key)
        val node = if (inserting)
            ctor().also { emitNode(it) }
        else useNode() as T
        B<T>(this, node).update()
        endNode()
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    val a = A(C())
    val str = "OK"
    var result = "fail"
    a.foo<String>(
        123,
        { "abc" },
        {
            bar(str) { }
            result = "OK"
        }
    )

    return result
}