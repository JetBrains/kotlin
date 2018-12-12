// IGNORE_BACKEND: JVM_IR

fun box() {
    lookAtMe {
        val c = "c"
    }
}

inline fun lookAtMe(f: (String) -> Unit) {
    val a = "a"
    f(a) // Should be no unneeded nops on this line, that might be generated for zero-parameters lambda
}

// 3 NOP