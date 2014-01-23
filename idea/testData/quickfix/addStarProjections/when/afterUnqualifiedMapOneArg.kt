// "class org.jetbrains.jet.plugin.quickfix.AddStarProjectionsFix" "false"
// ERROR: 2 type arguments expected
public fun foo(a: Any) {
    when (a) {
        is Map<Int> -> {}
        else -> {}
    }
}