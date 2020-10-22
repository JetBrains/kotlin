// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: WASM_NULL_CAST
// WITH_RUNTIME

import kotlin.test.assertEquals

fun stringMultiArray(): Array<Array<String>> = Array(3) {
    i -> Array(3) { j -> "$i-$j" }
}

fun stringNullableMultiArray(): Array<Array<String?>> = Array(3) {
    i -> if (i == 1) Array(3) { j -> "$i-$j" } as Array<String?> else arrayOfNulls<String>(3)
}

fun box(): String {
    val matrix = stringMultiArray()

    for (i in 0..2) {
        for (j in 0..2) {
            assertEquals("$i-$j", matrix[i][j], "matrix")
        }
    }

    val matrixNullable = stringNullableMultiArray()

    for (j in 0..2) {
        assertEquals(null, matrixNullable[0][j], "nullable")
        assertEquals("1-$j", matrixNullable[1][j], "nullable")
        assertEquals(null, matrixNullable[2][j], "nullable")
    }

    return "OK"
}
