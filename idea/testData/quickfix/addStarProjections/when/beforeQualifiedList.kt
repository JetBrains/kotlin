// "Add '<*>'" "true"
public fun foo(a: Any) {
    when (a) {
        is jet.List<caret> -> {}
        else -> {}
    }
}