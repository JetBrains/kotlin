// WITH_STDLIB

@NameBased
data class User(val name: String, val age: Int)

fun foo1(user: User) {
    val (<!WRONG_DESTRUCTURED_PROPERTY_NAME!>first<!>, <!WRONG_DESTRUCTURED_PROPERTY_NAME!>second<!>) = user
    val (<!UNNECESSARY_UNDERSCORE!>_<!>, name) = user
    val (age) = user
}

fun foo2(user: User) {
    val (age, <!WRONG_DESTRUCTURED_PROPERTY_NAME!>someName<!>) = user
}
