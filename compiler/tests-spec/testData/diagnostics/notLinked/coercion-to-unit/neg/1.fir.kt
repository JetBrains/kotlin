// TESTCASE NUMBER: 1

val y0 = when (2) {
    else -> if (true) {""}
}

val w:Any = TODO()

val y1 = when (2) {
    else -> if (true) {""} // false ok with coercion to Unit
}
