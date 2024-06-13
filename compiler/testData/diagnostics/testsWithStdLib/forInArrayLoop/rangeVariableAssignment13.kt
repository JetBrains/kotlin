// FIR_IDENTICAL
// LANGUAGE: +ProperForInArrayLoopRangeVariableAssignmentSemantic
// DIAGNOSTICS: -UNUSED_VALUE
// SKIP_TXT

fun testObjectArray() {
    var xs = arrayOf("a", "b", "c")
    for (x in xs) {
        println(x)
        xs = arrayOf("d", "e", "f")
    }
}

fun testPrimitiveArray() {
    var xs = intArrayOf(1, 2, 3)
    for (x in xs) {
        println(x)
        xs = intArrayOf(4, 5, 6)
    }
}