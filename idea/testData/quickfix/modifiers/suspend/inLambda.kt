// "Make test suspend" "true"
suspend fun foo() {}

inline fun bar(f: () -> Unit) {
}

fun test() {
    bar {
        <caret>foo()
    }
}