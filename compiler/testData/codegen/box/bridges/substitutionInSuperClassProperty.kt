open class A<T>(val t: T) {
    open val foo: T = t
}

open class B : A<String>("Fail")

class Z : B() {
    override val foo = "OK"
}


fun box() = (Z() : A<String>).foo
