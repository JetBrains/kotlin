// "Add '<*>'" "true"
public fun foo(a: Any) {
    when (a) {
        is kotlin.collections.List<caret> -> {}
        else -> {}
    }
}