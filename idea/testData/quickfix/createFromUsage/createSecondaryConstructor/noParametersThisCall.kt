// "Create secondary constructor" "true"
// ERROR: There's a cycle in the delegation calls chain
class Creation(val f: Int)
val v = Creation(<caret>)