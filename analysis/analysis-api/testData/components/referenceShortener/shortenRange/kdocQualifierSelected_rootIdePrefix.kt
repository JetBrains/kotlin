// FILE: main.kt
import dependency.Foo

/**
 * [<expr>_root_ide_package_.dependency.Foo</expr>.foo]
 */
fun test() {}

// FILE: dependency.kt
package dependency

object Foo {
    fun foo() {}
}
