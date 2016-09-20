// "Change return type of current function 'foo' to '(Long) -> Int'" "true"
fun foo(x: Any): Int {
    return {x: Long -> 42}<caret>
}