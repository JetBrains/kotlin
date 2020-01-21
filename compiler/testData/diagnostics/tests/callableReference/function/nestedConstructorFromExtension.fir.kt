// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KFunction0

class A {
    class Nested
}
    
fun A.main() {
    ::Nested
    val y = A::Nested
    
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction0<A.Nested>>(y)
}

fun Int.main() {
    <!UNRESOLVED_REFERENCE!>::Nested<!>
    val y = A::Nested

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction0<A.Nested>>(y)
}