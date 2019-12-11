fun f() {
    foo {
        bar {
            return@foo 1
        }
        return@foo 1
    }
}

fun foo(a: Any) {}
fun bar(a: Any) {}