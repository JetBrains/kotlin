// ISSUE: KT-72356
// FILE: a.kt
import kotlin.annotation.*

@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.FUNCTION)
annotation class An(val x: String)

// FILE: b.kt
open class C {
     @An(<!EVALUATED("1")!>"1"<!>)
     fun foo() {}
}

// FILE: c.kt
// 12345
fun <@An(<!EVALUATED("2")!>"2"<!>) Y> bar() {}
// The string constants have the same offsets in the two files.

class E : C()

fun box(): String {
    return "OK"
}
