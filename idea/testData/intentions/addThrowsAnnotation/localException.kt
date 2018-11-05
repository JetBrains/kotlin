// WITH_RUNTIME
// IS_APPLICABLE: false

fun a() {
    class Ex : RuntimeException()
    <caret>throw Ex()
}