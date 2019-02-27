// WITH_RUNTIME
package my.simple.name

fun main() {
    val a = kotlin<caret>.Int.MAX_VALUE
    val b = kotlin.Int.Companion.MAX_VALUE
    val c = kotlin.Int.Companion::MAX_VALUE
}