// FIR_IDENTICAL

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun test() = foo { "OK" }
