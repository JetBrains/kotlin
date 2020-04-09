// "Make test suspend" "true"
suspend fun foo() {}

inline fun bar(f: () -> Unit) {
}

inline fun baz(f: () -> Unit) {
}

fun test() {
    baz {
        bar {
            <caret>foo()
        }
    }
}
