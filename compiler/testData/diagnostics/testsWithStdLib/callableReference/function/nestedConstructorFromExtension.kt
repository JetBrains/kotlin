// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KFunction0

class A {
    class Nested
}
    
fun A.main() {
    ::<!UNRESOLVED_REFERENCE!>Nested<!>
    val y = A::Nested
    
    y : KFunction0<A.Nested>
}

fun Int.main() {
    ::<!UNRESOLVED_REFERENCE!>Nested<!>
    val y = A::Nested

    y : KFunction0<A.Nested>
}