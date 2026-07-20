// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM
import C.o

class C {
    companion {
        @JvmField
        val o = "O"
    }
}

@JvmField
companion val C.k = "K"

fun box(): String {
    return o + C.k
}
