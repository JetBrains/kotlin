sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int = when(this) {
        is Empty -> -1
        is Leaf  -> this.x
        is Node  -> this.left.max()
    }
}
