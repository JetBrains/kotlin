// COMPILATION_ERRORS
// It's found during working on binary expression optimizing KT-77993

fun testLowerPriority() {
    val y = x is Int <= true // Correct syntax: `<=` (COMPARISON) priority is lower than `is`
}

fun testEqualPriority() {
    val z = x is Int is Boolean // Correct syntax: `is` priority equals `is`
}

fun testHigherPriority() {
    val w = x is Int .. true // Incorrect syntax: `..` (RANGE) priority is higher than `is`
}
