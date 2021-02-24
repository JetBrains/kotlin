// PROBLEM: none

fun test(r: Runnable) {}

fun usage() {
    test(Runnable<caret> { return@Runnable })
}