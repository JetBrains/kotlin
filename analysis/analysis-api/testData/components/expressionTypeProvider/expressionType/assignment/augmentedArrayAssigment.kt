operator fun Array<String>.get(index1: Int, index2: Int) = this[index1 + index2]
operator fun Array<String>.set(index1: Int, index2: Int, elem: String) {
    this[index1 + index2] = elem
}

fun test(s: Array<String>) {
    <expr>s[2, -2]</expr> += "K"
}
