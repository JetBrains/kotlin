// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME
fun foo(s: String) {}

fun bar(s: String?) {
    foo(s<caret>.substring(1))
}