package codegen.enum.isFrozen

import kotlin.test.*
import konan.worker.*

enum class Zzz(val zzz: String) {
    Z1("z1"),
    Z2("z2")
}

@Test fun runTest() {
    println(Zzz.Z1.isFrozen)
}