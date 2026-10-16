// FILE: declarations.kt
@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs(val x: String, val number: Int)

typealias T = AnnoWithArgs

const val C = 1

fun f(): @T("x", C) String = ""

// FILE: main.kt
fun test() {
    <expr>f()</expr>
}
