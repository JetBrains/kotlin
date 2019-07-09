// FILE: 1.kt
package test

class AbstractTreeNode<T>(val value: T, val parent: AbstractTreeNode<T>?)

internal inline fun <reified T : Any> AbstractTreeNode<*>.findNotNullValueOfType(strict: Boolean = false): T {
    return findValueOfType(strict)!!
}

internal inline fun <reified T : Any> AbstractTreeNode<*>.findValueOfType(strict: Boolean = true): T? {
    var current: AbstractTreeNode<*>? = if (strict) this.parent else this
    while (current != null) {
        val value = current.value
        if (value is T) return value
        current = current.parent
    }
    return null
}

// FILE: 2.kt

import test.*

fun box(): String {
    return AbstractTreeNode("OK", null).findNotNullValueOfType<String>()!!
}