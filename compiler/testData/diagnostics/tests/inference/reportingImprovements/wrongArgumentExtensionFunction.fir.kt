// DIAGNOSTICS: -UNUSED_PARAMETER

class A
fun A.fn(b: Int): Nothing = TODO()

fun A.run() {
    "".apply { fn(<!ARGUMENT_TYPE_MISMATCH!>""<!>) }
}