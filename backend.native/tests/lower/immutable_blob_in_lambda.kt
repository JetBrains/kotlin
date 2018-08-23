package lower.immutable_blob_in_lambda

import kotlin.test.*

@Test
fun runTest() = run {
    val golden = immutableBlobOf(123)
    println(golden[0])
}