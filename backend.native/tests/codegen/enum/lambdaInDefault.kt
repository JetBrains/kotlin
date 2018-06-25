package codegen.enum.lambdaInDefault

import kotlin.test.*

enum class Zzz(val value: String.() -> Int = {
    length
}) {
    Q()
}

@Test fun runTest() {
    println(Zzz.Q)
}