// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetObjectDeclaration
// OPTIONS: usages

import kotlin.platform.platformStatic

class Foo {
    default <caret>object {
        fun f() {
        }

        platformStatic fun s() {
        }

        val CONST = 42
    }
}