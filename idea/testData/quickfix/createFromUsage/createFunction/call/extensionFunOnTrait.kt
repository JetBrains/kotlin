// "Create extension function 'foo'" "true"

interface T

fun test(t: T) {
    val b: Boolean = t.<caret>foo("1", 2)
}