sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int = when(this) {
        Empty -> -1
        is Leaf  -> this.x
        is Node  -> this.left.max()
    }

    fun maxIsClass(): Int = <!NO_ELSE_IN_WHEN!>when<!>(this) {
        Empty -> -1
        <!NO_COMPANION_OBJECT!>Leaf<!>  -> 0
        is Node  -> this.left.max()
    }

    fun maxWithElse(): Int = when(this) {
        is Leaf  -> this.x
        is Node  -> this.left.max()
        else -> -1
    }
}
