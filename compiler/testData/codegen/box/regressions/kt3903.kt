// IGNORE_BACKEND_FIR: JVM_IR
class Foo {
    fun bar(): String {
        fun <T> foo(t:() -> T) : T = t()
        foo { }
        return "OK"
    }
}

fun box(): String {
    return Foo().bar()
}
