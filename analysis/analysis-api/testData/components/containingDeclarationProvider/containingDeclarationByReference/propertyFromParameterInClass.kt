class SomeClass(
    val name: String,
    val age: Int,
)

fun test(someClass: SomeClass) {
    someClass.<caret>age
}
