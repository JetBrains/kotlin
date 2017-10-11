package codegen.controlflow.break1

import kotlin.test.*

@Test fun runTest() {
    loop@ while (true) {
        println("Body")
        break
    }
    println("Done")
}
