package coverage.basic.smoke0

data class User(val name: String)

fun main() {
    val user = User("Happy Kotlin/Native user")
    println(user)
}