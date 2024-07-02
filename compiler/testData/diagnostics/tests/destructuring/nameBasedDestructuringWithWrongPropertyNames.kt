// WITH_STDLIB

@NameBased
data class User(val name: String, val age: Int)

fun foo1(user: User) {
    val (first, second) = user
    val (_, name) = user
    val (age) = user
}

fun foo2(user: User) {
    val (age, someName) = user
}