// WITH_RUNTIME

fun foo() {
    listOf(1,2,3).map {<caret> Int::toString }
}