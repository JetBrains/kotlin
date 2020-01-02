// !WITH_NEW_INFERENCE
//KT-352 Function variable declaration type isn't checked inside a function body

package kt352

val f : (Any) -> Unit = {  -> }  //type mismatch

fun foo() {
    val f : (Any) -> Unit = { -> }  //!!! no error
}

class A() {
    val f : (Any) -> Unit = { -> }  //type mismatch
}

//more tests
val g : () -> Unit = { 42 }
val gFunction : () -> Unit = fun(): Int = 1

val h : () -> Unit = { doSmth() }

fun doSmth(): Int = 42
fun doSmth(a: String) {}

val testIt : (Any) -> Unit = {
    if (it is String) {
        doSmth(it)
    }
}