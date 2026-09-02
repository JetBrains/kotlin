// MODULE: m1
// FILE: main.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int, val s: String)

const val C = 1

class Usage(val list: <expr>@Anno(C, "a$C") String</expr>)

// MODULE: m2
// FILE: unrelated.kt
fun foo() {
    <caret_restoreAt>
}
