import A.foo
import A.bar

object A {
    fun foo() = "O"
    fun String.foo() = "K"

    @JvmStatic
    fun bar(s: Int) = "OK"
}

fun box(): String {
    val static = (::bar)(0)
    if (static != "OK") return "1"

    return (::foo)() + (String::foo)("")
}
