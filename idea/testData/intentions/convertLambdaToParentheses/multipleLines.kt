// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    listOf(1,2,3).map {<caret>
        println(it)
        Int::toString
    }
}