// IGNORE_BACKEND: JVM_IR
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

inline fun inlineMe(crossinline c : () -> Unit) = { c() }

class A {
    fun inlineSite() {
        inlineMe {
            assert(true)
        }
    }
}

// 1 GETSTATIC A\$inlineSite\$\$inlined\$inlineMe\$1.\$assertionsDisabled
