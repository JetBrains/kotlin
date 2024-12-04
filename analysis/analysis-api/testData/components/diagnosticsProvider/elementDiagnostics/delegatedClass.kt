// FILE: additionalFile.kt
interface Interface {
    fun <T> foo()
}

fun provider(): Interface = TODO()

abstract class AbstractClass : Interface by provider()

// FILE: main.kt
class Anoth<caret>er : AbstractClass()
