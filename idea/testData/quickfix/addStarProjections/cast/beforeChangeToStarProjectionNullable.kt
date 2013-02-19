// "Change type argument list to <*, *>" "true"
public fun foo(a: Any?) {
    a as Map<*, Int>?<caret>
}
