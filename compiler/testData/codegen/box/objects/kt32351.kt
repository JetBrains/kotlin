// IGNORE_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNKNOWN
// WITH_RUNTIME

interface Runnable {
    fun run()
}

class AnonymousClassInLambda {
    fun run(): Int {
        var x = 0
        val threads = (1..10).map {
            object : Runnable {
                override fun run() {
                    x++
                }
            }
        }
        threads.forEach { it.run() }
        return x
    }
}

fun box(): String {
    return if (AnonymousClassInLambda().run() == 10) "OK" else "Fail"
}
