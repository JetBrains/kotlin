package r

class MyClass {
    operator fun invoke() = this
    operator fun invoke(s: String) = this
}

fun foo(): Int = 1
fun foo(b: Boolean): Int = 2
val foo: MyClass = MyClass()

fun usages() {
    <expr>foo()</expr>
}
