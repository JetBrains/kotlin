class class_reassignment_2_Node {
    var left: class_reassignment_2_Node? = null
    var right: class_reassignment_2_Node? = null
    var value: Int = 0
}

fun class_reassignment_2(): Int {
    val root: class_reassignment_2_Node = class_reassignment_2_Node()
    root.value = 12
    root.left = class_reassignment_2_Node()
    var current: class_reassignment_2_Node? = root
    current = root.left
    current!!.value = 10593
    return current.value + root.value
}