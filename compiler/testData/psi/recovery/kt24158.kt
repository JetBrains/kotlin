// COMPILATION_ERRORS

fun main() {
    null + $foo.$bar.
}

fun foo2() {
    null + $foo. $bar . $baz .
}
