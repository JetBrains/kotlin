// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// LANGUAGE: +ContextParameters
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
public inline fun <T, R> myContext(with: T, block: context(T) () -> R): R {
    return block(with)
}

// MODULE: main(lib)
// FILE: main.kt
open class A
class B: A()

context(a: A) fun usage1(): A = a

fun context1(): A = myContext(B()) { <expr>usage1()</expr> }