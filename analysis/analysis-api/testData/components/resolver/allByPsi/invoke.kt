package r

class MyClass {
    operator fun invoke() = this

    fun self() = this
}

fun foo(): Int = 1
val foo: MyClass = MyClass()

val property get() = MyClass()

fun usages() {
    foo()
    foo
    (((foo)())()).invoke()
    foo.invoke()
    property()
}