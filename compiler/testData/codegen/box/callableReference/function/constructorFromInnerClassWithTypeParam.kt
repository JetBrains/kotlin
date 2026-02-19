var result = "failed"

class Foo<T> {
    inner class Bar<T> {
        constructor() {
            result = "OK"
        }
    }
}

fun box(): String {
    val a: Foo<*>.() -> Foo<out Any?>.Bar<out String> = Foo<out Any?>::Bar
    Foo<Int>().a()
    return result
}