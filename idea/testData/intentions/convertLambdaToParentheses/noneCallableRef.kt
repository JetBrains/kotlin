// IS_APPLICABLE: false
// WITH_RUNTIME

fun foo() {
    listOf(1,2,3).map {<caret> println(it) }
}