sealed class Tree {
    object Empty: Tree()
    class Leaf(val x: Int): Tree()
    class Node(val left: Tree, val right: Tree): Tree()

    fun max(): Int {
        when(this) {
            is Empty -> return -1
            is Leaf -> return <!DEBUG_INFO_SMARTCAST!>this<!>.x
            is Node -> return <!DEBUG_INFO_SMARTCAST!>this<!>.left.max()
        }
    }
}
