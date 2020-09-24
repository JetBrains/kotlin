package codegen.basics.k42000_2

import kotlin.test.*

// https://youtrack.jetbrains.com/issue/KT-42000

@Test
fun runTest() {
    assertFailsWith<Error> {
        when (1) {
            else -> throw Error()
        } as String
    }
}
