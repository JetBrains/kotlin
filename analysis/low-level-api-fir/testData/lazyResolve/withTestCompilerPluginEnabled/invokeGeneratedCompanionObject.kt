// ISSUE: KT-84711
// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT
package pack

import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
interface Foo {
    val bar: String
}

operator fun Foo.Companion.invoke(action: Foo.() -> Unit) {}

fun main() {
    Foo {
        b<caret>ar
    }
}
