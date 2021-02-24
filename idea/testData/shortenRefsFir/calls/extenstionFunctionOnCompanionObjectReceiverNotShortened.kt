// FIR_COMPARISON
package test

class T {
    companion object
}

fun T.Companion.ext() {}

fun usage() {
    <selection>T.ext()</selection>
}