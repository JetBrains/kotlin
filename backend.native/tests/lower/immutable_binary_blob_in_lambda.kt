package lower.immutable_binary_blob_in_lambda

import kotlin.test.*

@Test
fun runTest() = run {
    val golden = immutableBinaryBlobOf(123)
    println(golden[0])
}