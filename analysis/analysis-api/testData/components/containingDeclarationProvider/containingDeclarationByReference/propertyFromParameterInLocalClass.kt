fun test() {
    class SomeClass(
        val name: String,
        val age: Int,
    )

    val someClass = SomeClass("", 5)
    someClass.<caret>age
}
