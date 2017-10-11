package codegen.function.boolean

import kotlin.test.*

fun bool_yes(): Boolean = true

@Test fun runTest() {
    if (!bool_yes()) throw Error()
}
