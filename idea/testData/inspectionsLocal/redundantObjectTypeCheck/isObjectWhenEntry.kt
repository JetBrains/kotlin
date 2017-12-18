object O

fun foo(arg: Any) {
    when (arg) {
        <caret>is O -> {
        }
    }
}