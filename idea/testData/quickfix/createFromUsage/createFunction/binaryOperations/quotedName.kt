// "Create extension function 'Int.!u00A0'" "true"
// WITH_RUNTIME

fun test() {
    val t: Int = 1 <caret>`!u00A0` 2
}