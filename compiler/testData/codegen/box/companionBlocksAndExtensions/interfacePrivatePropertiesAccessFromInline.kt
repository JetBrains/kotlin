// LANGUAGE: +CompanionBlocks

// FILE: I.kt

package pkg1

interface I {
    companion {
        private var secret = "x"
        internal inline fun readSecret() = secret
        internal inline fun writeSecret(v: String) { secret = v }
    }
}

// FILE: test.kt

package pkg2

import pkg1.I

fun box(): String {
    I.writeSecret("OK")
    return I.readSecret()
}
