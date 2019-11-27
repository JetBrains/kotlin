// IGNORE_BACKEND_FIR: JVM_IR
open class A<T>(val t: T) {
    open val foo: T = t
}

open class B : A<String>("Fail")

class Z : B() {
    override val foo = "OK"
}


fun box(): String {
    val a: A<String> = Z()
    return a.foo
}
