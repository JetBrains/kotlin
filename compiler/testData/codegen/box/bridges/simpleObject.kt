open class A<T> {
    open fun foo(t: T) = "A"
}

object Z : A<String>() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val z = object : A<String>() {
        override fun foo(t: String) = "z"
    }
    return when {
        Z.foo("")               != "Z" -> "Fail #1"
        z.foo("")               != "z" -> "Fail #2"
        (Z : A<String>).foo("") != "Z" -> "Fail #3"
        (z : A<String>).foo("") != "z" -> "Fail #4"
        else -> "OK"
    }
}
