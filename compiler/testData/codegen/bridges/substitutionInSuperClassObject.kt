open class A<T> {
    open fun foo(t: T) = "A"
}

open class B : A<String>()

object Z : B() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val o = object : B() {
        override fun foo(t: String) = "o"
    }
    return when {
        Z.foo("")               != "Z" -> "Fail #1"
        o.foo("")               != "o" -> "Fail #2"
        (Z : B).foo("")         != "Z" -> "Fail #3"
        (o : B).foo("")         != "o" -> "Fail #4"
        (Z : A<String>).foo("") != "Z" -> "Fail #5"
        (o : A<String>).foo("") != "o" -> "Fail #6"
        else -> "OK"
    }
}
