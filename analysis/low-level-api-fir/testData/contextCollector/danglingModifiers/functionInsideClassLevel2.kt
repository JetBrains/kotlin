package foo

annotation class Anno(val value: String)

class MyClass {
    @Anno(
        action {
            fun bar(i: Int, s: String): Long {
                val l = 1L
                return <expr>l</expr>
            }
        }
    )
}

fun <T> action(body: () -> T) : T = body()