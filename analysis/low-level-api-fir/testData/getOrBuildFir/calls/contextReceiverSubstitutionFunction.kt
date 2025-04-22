// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// LANGUAGE: +ContextReceivers
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
context(A)
public inline fun <A> myContextOf(): A = this@A

// MODULE: main(lib)
// FILE: main.kt
class MyClass

val withContextProp : context(MyClass)() -> Unit = {
    <expr>myContextOf<MyClass>() == MyClass()</expr>
}
