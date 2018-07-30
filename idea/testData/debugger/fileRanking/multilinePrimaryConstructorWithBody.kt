//FILE: a/a.kt
class A(
    val firstName: String,
    val lastName: String,
    val age: Int
) {
    val c = 1
    val d = "A"
}

//FILE: b/a.kt
class B(
    val firstName: String,
    val lastName: String,
    val age: Int
) {
    init {
        val a = 5
        val b = 6
    }
}