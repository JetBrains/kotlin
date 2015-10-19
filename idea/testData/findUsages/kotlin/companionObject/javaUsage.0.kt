// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages

import kotlin.platform.platformStatic

class Foo {
    companion <caret>object {
        fun f() {
        }

        @platformStatic fun s() {
        }

        val CONST = 42
    }
}