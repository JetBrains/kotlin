
// FILE: sealed.kt
package s

sealed class Node
class Leaf(val value: Int) : Node()
class Branch(val left: Node, val right: Node) : Node()

fun <T : Node> eval(n: T): Int =
	when (n) {
		is Leaf -> n.value
		is Branch -> eval(n.left) + eval(n.right)
	}

// FILE: test.kt
import s.Leaf
import s.Branch
import s.eval

fun box(): String {
	val tree = Branch(Leaf(1), Branch(Leaf(2), Leaf(3)))
	return eval(tree).toString()
}

