// FILE: main.kt
import dependency.Foo

/**
 * [<expr>dependency.Foo.unresolved</expr>]
 */
fun test() {}

// FILE: dependency.kt
package dependency

class Foo
