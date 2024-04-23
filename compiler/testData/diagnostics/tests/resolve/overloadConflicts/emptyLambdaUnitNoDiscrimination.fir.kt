// ISSUE: KT-63596

fun e(block: () -> String): String = ""
fun e(block: () -> Unit): Int = 0
fun c(block: (x: Int) -> String): String = ""
fun c(block: (x: Int) -> Unit): Int = 0

fun test() {
    e {}
    c {}
}