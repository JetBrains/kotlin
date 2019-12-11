// !LANGUAGE: -ProhibitLocalAnnotations

fun f() {
    annotation class Anno

    @Anno class Local {
        annotation class Nested
    }
}
