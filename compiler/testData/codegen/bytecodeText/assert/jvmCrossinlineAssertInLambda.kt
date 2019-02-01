// IGNORE_BACKEND: JVM_IR
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm

inline fun inlineMe(crossinline c : () -> Unit) = {
    assert(true)
    c()
}

class A {
    fun inlineSite() {
        inlineMe { }
    }
}

// inlineSite:
// 1 GETSTATIC A\$inlineSite\$\$inlined\$inlineMe\$1.\$assertionsDisabled
// A.<clinit>:
// 1 LDC LA\$inlineSite\$\$inlined\$inlineMe\$1;.class\s*INVOKEVIRTUAL java/lang/Class.desiredAssertionStatus \(\)Z
// 1 PUTSTATIC A\$inlineSite\$\$inlined\$inlineMe\$1.\$assertionsDisabled : Z
// in declaration site and in inline site
// 2 INVOKEVIRTUAL java/lang/Class.desiredAssertionStatus \(\)Z
