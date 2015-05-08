// "Create extension function 'foo'" "true"

trait T

fun test(t: T) {
    val b: Boolean = t.<caret>foo("1", 2)
}