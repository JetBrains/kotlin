// ISSUE: KT-67983

// FILE: Crashing.kt
package crashing

abstract class Node<N: Node<N>> {
    abstract val next: List<N>
}

typealias ReadOnlyNode = <!RECURSIVE_TYPEALIAS_EXPANSION!>Node<out ReadOnlyNode><!>

// FILE: Ok.kt
package ok

abstract class Node<out N: Node<N>> {
    abstract val next: List<N>
}

typealias ReadOnlyNode = <!RECURSIVE_TYPEALIAS_EXPANSION!>Node<ReadOnlyNode><!>
