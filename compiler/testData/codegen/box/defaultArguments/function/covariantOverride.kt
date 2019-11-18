// IGNORE_BACKEND_FIR: JVM_IR
open class Foo {
    open fun foo(x: CharSequence = "O"): CharSequence = x
}
class Bar(): Foo() {
    override fun foo(x: CharSequence): String {   // Note the covariant return type
        return x.toString() + "K"
    }
}

fun box() = Bar().foo()
