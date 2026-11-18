// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
package pack

import org.jetbrains.kotlin.plugin.sandbox.WithCompanion

interface Base {
    fun value(): String = "OK"
}

@WithCompanion(Base::class)
class Owner {
}

fun main() {
    Owner.val<caret>ue()
}
