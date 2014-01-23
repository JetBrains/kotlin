class A {
    var a: String = "Fail"

    {
        open class B() {
            open fun s() : String = "O"
        }

        object O: B() {
            override fun s(): String = "K"
        }

        a = B().s() + O.s()
    }
}

fun box() : String {
    return A().a
}