// "class org.jetbrains.kotlin.idea.quickfix.AddStarProjectionsFix" "false"
// ERROR: 2 type arguments expected for interface Map<K, out V>
public fun foo(a: Any) {
    when (a) {
        is <caret>Map<Int> -> {}
        else -> {}
    }
}
