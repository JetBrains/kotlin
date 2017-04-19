// WITH_RUNTIME
// PROBLEM: none

fun foo() {
    listOf(1,2,3).map {<caret>
        println(it)
        Int::toString
    }
}