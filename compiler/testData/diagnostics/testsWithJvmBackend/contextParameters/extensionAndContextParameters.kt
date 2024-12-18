// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A
class B

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>)<!>
fun foo() {}

fun A.foo() { }

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>, b: <!DEBUG_INFO_MISSING_UNRESOLVED!>B<!>)<!>
fun bar(){}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>)<!>
fun B.bar(){}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>, b: <!DEBUG_INFO_MISSING_UNRESOLVED!>B<!>)<!>
fun qux(){}

fun A.qux(<!UNUSED_PARAMETER!>b<!>: B){}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>)<!>
val b: String
    get() = ""

val A.b: String
    get() = ""
