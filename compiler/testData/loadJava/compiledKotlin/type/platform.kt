// TARGET_BACKEND: JVM
// ALLOW_AST_ACCESS
package test

import java.util.*

fun printStream() = System.out
fun list() = Collections.emptyList<String>()
fun array(a: Array<Int>) = Arrays.copyOf(a, 2)
