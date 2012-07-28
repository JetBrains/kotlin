//KT-352 Function variable declaration type isn't checked inside a function body

package kt352

val f : (Any) -> Unit = <!TYPE_MISMATCH!>{ () : Unit -> }<!>  //type mismatch

fun foo() {
    val <!UNUSED_VARIABLE!>f<!> : (Any) -> Unit = <!TYPE_MISMATCH!>{ () : Unit -> }<!>  //!!! no error
}

class A() {
    val f : (Any) -> Unit = <!TYPE_MISMATCH!>{ () : Unit -> }<!>  //type mismatch
}

//more tests
val g : () -> Unit = <!TYPE_MISMATCH!>{ (): Int -> 42 }<!>

val h : () -> Unit = { doSmth() }

fun doSmth(): Int = 42
fun doSmth(<!UNUSED_PARAMETER!>a<!>: String) {}

val testIt : (Any) -> Unit = {
    if (it is String) {
        doSmth(it)
    }
}
