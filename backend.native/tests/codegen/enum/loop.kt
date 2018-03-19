package codegen.enum.loop

import kotlin.test.*

enum class Zzz {
    Z {
        init {
            println(Z.name)
        }
    }
}

@Test fun runTest() {
    println(Zzz.Z)
}