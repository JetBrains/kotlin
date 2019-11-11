fun interface Foo

fun interface Foo {
    fun invoke()
}

private fun interface Foo

@Bar
fun interface Foo

class TopLevel {
    fun interface Foo
}

fun
interface Foo