// LANGUAGE: +CompanionBlocksAndExtensions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
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
