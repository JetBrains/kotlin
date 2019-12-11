//KT-455 Do not repeat errors in definite assignment checks

package kt455

fun foo() {
    val a: Int
    doSmth(a)   //error
    doSmth(a)   //no repeat error
}
fun doSmth(i: Int) {}
