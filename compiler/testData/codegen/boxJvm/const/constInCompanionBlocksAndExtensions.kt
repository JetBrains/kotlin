// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: ANDROID
// TARGET_BACKEND: JVM
// WITH_REFLECT

@Ann(C.o + C.k)
class C {
    companion {
        const val o = "O"
    }
}

annotation class Ann(val x: String)

companion const val C.k = "K"

fun box(): String {
    return (C::class.annotations.single() as Ann).x
}
