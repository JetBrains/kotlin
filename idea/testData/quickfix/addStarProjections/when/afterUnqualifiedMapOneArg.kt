// "Add '<*, *>'" "false"
// "Add '<*>'" "false"
// ERROR: 2 type arguments expected
public fun foo(a: Any) {
    when (a) {
        is Map<Int> -> {}
        else -> {}
    }
}