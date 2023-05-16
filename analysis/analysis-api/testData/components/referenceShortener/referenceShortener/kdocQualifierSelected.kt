// FILE: main.kt
import dependency.Foo

/**
 * [<expr>dependency.Foo</expr>.foo]
 */
fun test() {}

// FILE: dependency.kt
package dependency

object Foo {
    fun foo() {}
}
