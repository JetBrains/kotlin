// LANGUAGE: -ProperForInArrayLoopRangeVariableAssignmentSemantic
// DIAGNOSTICS: -UNUSED_VALUE
// SKIP_TXT

fun testObjectArray() {
    var xs = arrayOf("a", "b", "c")
    for (x in xs) {
        println(x)
        <!ASSIGNMENT_TO_ARRAY_LOOP_VARIABLE!>xs<!> = arrayOf("d", "e", "f")
    }
}

fun testPrimitiveArray() {
    var xs = intArrayOf(1, 2, 3)
    for (x in xs) {
        println(x)
        <!ASSIGNMENT_TO_ARRAY_LOOP_VARIABLE!>xs<!> = intArrayOf(4, 5, 6)
    }
}

var global = arrayOf("a", "b", "c")

fun testGlobalArray() {
    for (x in global) {
        println(x)
        global = arrayOf("d", "e", "f")
    }
}

fun testAssignmentNotInLoop() {
    var xs = intArrayOf(1, 2, 3)
    println(xs)
    xs = intArrayOf(7, 8, 9)
    for (x in xs) {
        println(x)
    }
    xs = intArrayOf(4, 5, 6)
    println(xs)
}