// COMPILATION_ERRORS

class Foo {
    companion object {
        operator fun invoke(param: Int) = Foo()
    }
}

fun test() {
    <expr>Foo(param)</expr>
}