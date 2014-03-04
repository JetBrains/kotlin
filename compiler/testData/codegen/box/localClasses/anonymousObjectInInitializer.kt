class A {
    var a: String = "Fail"

    {
        a = object {
            override fun toString(): String = "OK"
        }.toString()
    }
}

fun box() : String {
    return A().a
}