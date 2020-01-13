// WITH_RUNTIME
fun foo(f: (Map.Entry<Int, Int>) -> Int) {}

fun bar() {
    foo { <caret>it ->
        it.key + it.value
    }
}