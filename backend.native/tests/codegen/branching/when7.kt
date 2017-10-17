package codegen.branching.when7

import kotlin.test.*

@Test fun runTest() {
    main(emptyArray())
}

fun main(args: Array<String>) {
    val b = args.size < 1
    val x = if (b) Any() else throw Error()
}