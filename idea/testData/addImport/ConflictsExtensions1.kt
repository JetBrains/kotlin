//IMPORT: conflicts.extensions.foo5
package p

import conflicts.extensions.foo1
import conflicts.extensions.foo2
import conflicts.extensions.foo3
import conflicts.extensions.foo4
import conflicts.extensions.deps.*

fun main(args: Array<String>) {
    val b: Byte = 1
    b.inv(255)
}