// PROBLEM: none
// WITH_RUNTIME
fun test(map: Map<Int, String>) {
    map.<caret>forEach { (key, value) ->
        foo(key, value)
    }
}

fun foo(i: Int, s: String) {}