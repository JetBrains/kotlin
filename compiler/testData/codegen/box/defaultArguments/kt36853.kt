interface IFoo {
    fun foo(): String
}

tailrec fun tailrecDefault(
    fake: Int,
    x: IFoo = object : IFoo {
        override fun foo(): String = "OK"
    }
): String {
    return if (fake == 0)
        tailrecDefault(1)
    else
        x.foo()
}

fun box(): String = tailrecDefault(0)
