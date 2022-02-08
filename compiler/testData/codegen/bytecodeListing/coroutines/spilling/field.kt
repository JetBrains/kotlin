// WITH_STDLIB

interface Result

interface Foo {
    val Result.value: Any
        get() = TODO()
}

fun use(c: suspend Foo.() -> Unit) {}

fun generate(): Result = TODO()

fun test() {
    use {
        val value = generate().value
    }
}