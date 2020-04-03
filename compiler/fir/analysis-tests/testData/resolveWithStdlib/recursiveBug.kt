class Foo(name: () -> String) {
    val result = run { name() }

    val name = result.length
}

fun bar(name: () -> String) {
    val result = run { name() }

    val name = result.length
}