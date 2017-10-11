package codegen.controlflow.for_loops_coroutines

import kotlin.test.*

import kotlin.coroutines.experimental.*

@Test fun runTest() {
    val sq = buildSequence {
        for (i in 0..6 step 2) {
            print("before: $i ")
            yield(i)
            println("after: $i")
        }
    }
    println("Got: ${sq.joinToString(separator = " ")}")
}