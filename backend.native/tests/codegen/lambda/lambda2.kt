package codegen.lambda.lambda2

import kotlin.test.*

@Test fun runTest() {
    main(arrayOf("arg0"))
}

fun main(args : Array<String>) {
    run {
        println(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}