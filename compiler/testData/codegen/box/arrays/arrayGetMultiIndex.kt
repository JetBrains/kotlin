// IGNORE_BACKEND_FIR: JVM_IR
operator fun Array<String>.get(index1: Int, index2: Int) = this[index1 + index2]
operator fun Array<String>.set(index1: Int, index2: Int, elem: String) {
    this[index1 + index2] = elem
}

fun box(): String {
    val s = Array<String>(1, { "" })
    s[1, -1] = "OK"
    return s[-2, 2]
}
 
