// COMPILATION_ERRORS

package test

fun post(action: (Unit) -> Unit) {}
fun <T> post(action: (T) -> Unit) {}

fun usage() {
    <caret>post {}
}