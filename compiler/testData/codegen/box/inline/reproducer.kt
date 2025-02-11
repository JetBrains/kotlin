// WITH_STDLIB

// FILE: ImmutableListTest.kt
class ImmutableListTest {
    fun persistentListFails() {
        (1..1885).map { it }.toTypedArray()
    }
}

// FILE: ImmutableMapTest.kt
class ImmutableHashMapTest {
    fun regressionGithubIssue114() {
        val e = Array(101) { it }.map { it to it }
        e.toTypedArray()
    }
}

// FILE: Main.kt
fun box() : String {
    ImmutableHashMapTest().regressionGithubIssue114()
    ImmutableListTest().persistentListFails()
    return "OK"
}
