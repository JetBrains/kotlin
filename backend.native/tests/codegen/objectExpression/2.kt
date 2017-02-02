fun main(args: Array<String>) {
    val a = "a"

    val x = object {
        override fun toString(): String {
            return foo {
                a
            }
        }

        fun foo(lambda: () -> String) = lambda()
    }

    print(x)
}

fun print(x: Any) = println(x.toString())