// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!UNSUPPORTED!>context(_: String)<!>
class C {
    context(_: String)
    constructor() {}
}

class C2 {
    context(_: String)
    constructor() {}
}

<!UNSUPPORTED!>context(_: String)<!>
interface I

<!UNSUPPORTED!>context(_: String)<!>
enum class E

<!UNSUPPORTED!>context(_: String)<!>
annotation class A

<!UNSUPPORTED!>context(_: String)<!>
object O

context(_: String)
typealias TA = Any

val objectExpression = <!UNRESOLVED_REFERENCE!>context<!>(<!UNRESOLVED_REFERENCE!>_<!><!SYNTAX!>: String<!>)<!SYNTAX!><!> object<!SYNTAX!><!> {}
