// "Change 'foo' function return type to '(Long) -> Int'" "true"
fun foo(x: Any): Int {
    return {(x: Long) -> 42}<caret>
}