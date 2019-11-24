fun test() = foo({ line: String -> line })

fun <T> foo(x: T): T = TODO()

fun box(): String {
    return "OK"
}