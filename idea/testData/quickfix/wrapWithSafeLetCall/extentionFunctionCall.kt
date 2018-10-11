// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

fun f(s: String, action: (String.() -> Unit)?) {
    s.action<caret>()
}