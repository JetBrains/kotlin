// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// WITH_STDLIB

class A

<!UNSUPPORTED!>context(c: A)<!>
class Test1

<!UNSUPPORTED!>context(c: A)<!>
object Test2

<!UNSUPPORTED!>context(c: A)<!>
interface Test3

<!UNSUPPORTED!>context(c: A)<!>
fun interface Test4 {
    fun foo()
}

<!UNSUPPORTED!>context(c: A)<!>
data class Test5(val a: Int)

<!UNSUPPORTED!>context(c: A)<!>
data object Test6 {}

<!UNSUPPORTED!>context(c: A)<!>
enum class Test7 {
    <!UNSUPPORTED!>context(c: A)<!>
    FIRST
}

<!UNSUPPORTED!>context(c: A)<!>
@JvmInline
value class Test8(val a: Int)

<!UNSUPPORTED!>context(c: A)<!>
annotation class Test9

<!UNSUPPORTED!>context(c: A)<!>
class Test10<T>

class Test11 {
    <!UNSUPPORTED!>context(c: A)<!>
    class Nested

    <!UNSUPPORTED!>context(c: A)<!>
    inner class Inner

    <!UNSUPPORTED!>context(c: A)<!>
    object Obj

    <!UNSUPPORTED!>context(c: A)<!>
    companion object { }
}

val test12 = <!UNRESOLVED_REFERENCE!>context<!>(<!UNRESOLVED_REFERENCE!>c<!><!SYNTAX!>: A<!>)<!SYNTAX!><!> object<!SYNTAX!><!> { }

<!UNSUPPORTED!>context(c: A)<!>
typealias Test13 = String