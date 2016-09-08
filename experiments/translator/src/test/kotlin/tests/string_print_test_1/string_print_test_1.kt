fun string_print_test_1_inline(): Int {
    println("STRING INLINE PRINT WORKS")
    return 23
}

fun string_print_test_1_variable(): Int {
    val x = "STRING VARIABLE WORKS"
    val y = x
    println(y)
    return 1
}