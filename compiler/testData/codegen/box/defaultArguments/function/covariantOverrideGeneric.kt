// IGNORE_BACKEND_FIR: JVM_IR
open class Foo {
    open fun foo(x: CharSequence = "O"): CharSequence = x
}
class Bar<T : String>: Foo() {
    override fun foo(x: CharSequence): T {   // Note the covariant return type
        return (x.toString() + "K") as T
    }
}

fun box() = Bar<String>().foo()
