data class User private constructor(val name: String) {
    companion object {
        fun create(name: String): User = User(name)
    }
}

fun copyFromOutside(user: User): User {
    return user.copy(name = "test")
}
