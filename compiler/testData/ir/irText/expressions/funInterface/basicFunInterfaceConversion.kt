// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57428

fun interface Foo {
    fun invoke(): String
}

fun foo(f: Foo) = f.invoke()

fun test() = foo { "OK" }
