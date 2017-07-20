// "Add non-null asserted (!!) call" "true"
// WITH_RUNTIME

class A {
    fun foo(): List<Int?> = listOf()

    fun bar(i : Int, s: String) = Unit

    fun use() {
        val a = A()
        a.bar(a.foo().<caret>single(), "Asd")
    }
}