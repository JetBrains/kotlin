fun Int.foo() = 239
fun Long.bar() = 239.toLong()

fun box(): String {
    42?.foo()
    42.toLong()?.bar()
    return "OK"
}
