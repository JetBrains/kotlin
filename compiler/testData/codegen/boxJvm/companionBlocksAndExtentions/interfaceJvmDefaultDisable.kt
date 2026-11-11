// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: disable
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

// Companion block members of an interface compiled with -jvm-default=disable, where interface method bodies are
// compiled into DefaultImpls.

interface I {
    // Compiled into DefaultImpls, calls the statics of the interface.
    fun d() = "d" + withDefaults() + next()

    companion {
        fun withDefaults(a: Int = 1) = "$a"
        inline fun transform(x: Int = 1, f: (Int) -> Int = { it * 10 }) = f(x)

        private var counter = 0
        fun next() = ++counter
    }
}

class Impl : I

companion fun I.create(): I = Impl()

fun box(): String {
    if (I.create().d() != "d11") return "Fail: d"
    if (I.withDefaults(2) != "2") return "Fail: withDefaults"
    if (I.transform() != 10 || I.transform(2) { it + 1 } != 3) return "Fail: transform"
    if (I.next() != 2) return "Fail: next"
    return "OK"
}
