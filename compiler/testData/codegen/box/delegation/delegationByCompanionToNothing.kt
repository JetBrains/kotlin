// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK
// MODULE: lib
// NO_COMMON_FILES
// FILE: lib.kt
interface II {
    companion object : DDD by error("OK")
}

interface DDD {
    fun bar(d: String = error("FAIL4")): String
}

// MODULE: main(lib)
// NO_COMMON_FILES
// FILE: main.kt
fun box() : String {
    @Suppress("INVISIBLE_REFERENCE")
    try {
        return II.bar()
    } catch (e: ExceptionInInitializerError) {
        return (e.cause as? IllegalStateException)?.message ?: "FAIL 2"
    }

    return "FAIL"
}
