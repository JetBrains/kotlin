// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ExpectedTypeGuidedResolution

sealed interface Tree {
    data object Leaf: Tree
    data class Node(val left: Tree, val info: Int, val right: Tree)
}

val leaf: Tree = Leaf

fun create(n: Int): Tree = when (n) {
    0 -> Leaf
    else -> Tree.Node(Leaf, n, Leaf)
}
