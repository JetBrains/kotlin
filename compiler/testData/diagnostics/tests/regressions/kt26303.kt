// FILE: a.kt

package foo

operator fun Int.invoke() = null
fun Int.foo() = null

// FILE: b.kt

package bar

import foo.foo as invoke
import foo.invoke

fun main(args: Array<String>) {
    42()
}