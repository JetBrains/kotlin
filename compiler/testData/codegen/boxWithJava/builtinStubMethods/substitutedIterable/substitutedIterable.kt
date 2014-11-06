trait MyIterable<T> : Iterable<T>

class E : RuntimeException()
fun foo(): MyIterable<String> = throw E()

fun box(): String {
    try {
        foo().iterator().next()
        return "Fail: E should have been thrown"
    } catch (e: E) {}

    Test.checkCallFromJava()

    return "OK"
}
