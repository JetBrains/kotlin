package codegen.dataflow.scope1

import kotlin.test.*

var b = true

@Test fun runTest() {
    var x = 1
    if (b) {
        var x = 2
    }
    println(x)
}
