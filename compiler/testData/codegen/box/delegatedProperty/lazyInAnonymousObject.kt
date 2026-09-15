// ISSUE: KT-89384
// IGNORE_BACKEND: WASM
// WITH_STDLIB
// DUMP_IR
interface Matcher {
    fun check(): Boolean
}

class Config {
    fun createMatcher(pattern: String): Matcher = object : Matcher {
        private val suffix: String? by lazy {
            pattern.takeIf { it == "x" }
        }

        override fun check(): Boolean {
            return suffix != null
        }
    }
}

fun box(): String {
    Config().createMatcher("x").check()
    return "OK"
}
