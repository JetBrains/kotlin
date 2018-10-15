// PROBLEM: none
fun foo(f: (String?) -> Int) {}

fun test() {
    foo(fun(it: String?): Int {
        if (it != null) return@foo<caret> 1
        return 0
    })
}