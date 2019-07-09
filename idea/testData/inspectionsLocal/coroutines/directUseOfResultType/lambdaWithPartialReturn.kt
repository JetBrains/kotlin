// PROBLEM: none
package kotlin

fun test(arg: Boolean) {
    val x = foo@<caret>{
        if (!arg) {
            return@foo Result(true)
        } else {
            Result(false)
        }
    }
}
