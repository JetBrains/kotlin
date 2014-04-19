fun test() {
    val somelong = 1 + 2 +
            3 - 4 -
            5 * 6 *
                    7 / 8 /
                    9 % 10 %
                    11

    val withBrackets = 3 +
            4 - (5 +
            4 * 5)
}

// SET_TRUE: ALIGN_MULTILINE_BINARY_OPERATION
// Strage behaviour for disabled alignment is same to Java
