package org.jetbrains.kotlin.runner

fun <T> Set.Companion.build(size: Int, init: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.build(size: Int, init: CollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun main() {
    val a: Set<Number> = [1, 2, 3.0]
}