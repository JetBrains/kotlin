sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int {
        <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when(this) {
            is Empty -> return -1
            is Leaf -> return <!DEBUG_INFO_SMARTCAST!>this<!>.x
            is Node -> return <!DEBUG_INFO_SMARTCAST!>this<!>.left.max()
        }<!>
    }
}
