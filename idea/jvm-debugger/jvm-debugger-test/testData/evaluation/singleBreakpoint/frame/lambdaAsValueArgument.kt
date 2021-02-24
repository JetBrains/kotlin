package lambdaAsValueArgument

fun main() {
    contactMapper.toString()
}

object MyContext

data class User(val firstName: String, val lastName: String, val age: Int)

fun foo(block: User.(User) -> Unit) {
    val item = User("a", "b", 10)
    item.block(item)
}

val contactMapper = MyContext.run {
    foo({ i ->
        //Breakpoint!
        this.copy(
            firstName = "Foo",
            lastName = "Bar",
            age = 100
        )
    })
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES