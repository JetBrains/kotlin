// PROBLEM: none
// WITH_RUNTIME
fun test(): List<String> {
    return listOf(1, 2, 3).<caret>mapNotNull { i ->
        foo {
            bar(i)
        }
    }
}

fun <T> foo(f: () -> T): T = f()

fun bar(i: Int): String? = null