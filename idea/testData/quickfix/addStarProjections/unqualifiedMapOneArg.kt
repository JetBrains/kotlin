// "class org.jetbrains.kotlin.idea.quickfix.AddStarProjectionsFix" "false"
// ERROR: 2 type arguments expected for interface Map<K, out V> defined in kotlin.collections
public fun foo(a: Any) {
    a is <caret>Map<Int>
}
