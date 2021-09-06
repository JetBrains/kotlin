interface IFoo {
    fun foo(): String
}

tailrec fun tailrecDefault(
    fake: Int,
    x: IFoo = object : IFoo {
        tailrec fun tailrecDefaultNested(
            b: Boolean,
            y: IFoo = object: IFoo {
                override fun foo() = "OK"
            }
        ): String {
            return if (b)
                tailrecDefaultNested(false)
            else
                y.foo()
        }

        override fun foo(): String {
            return tailrecDefaultNested(true)
        }
    }
): String {
    return if (fake == 0)
        tailrecDefault(1)
    else
        x.foo()
}

fun box(): String = tailrecDefault(0)
