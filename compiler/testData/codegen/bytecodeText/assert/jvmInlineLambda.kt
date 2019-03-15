// IGNORE_BACKEND: JVM_IR
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

inline fun inlineMe(c: () -> Unit) = c()

class A {
    fun inlineSite() {
        inlineMe {
            assert(true)
        }
    }
}

// 1 GETSTATIC A.\$assertionsDisabled
