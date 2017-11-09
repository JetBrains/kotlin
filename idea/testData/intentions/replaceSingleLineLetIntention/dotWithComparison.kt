// IS_APPLICABLE: false
// WITH_RUNTIME

fun main(args: Array<String>) {
    args[0].let<caret> { it.isBlank() && it.toByteOrNull() != null }
}