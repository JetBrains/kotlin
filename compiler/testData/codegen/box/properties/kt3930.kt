// IGNORE_BACKEND_FIR: JVM_IR
public abstract class Foo {
    var isOpen = true
        private set
}
public class Bar: Foo() {
    inner class Baz {
        fun call() {
            val s = this@Bar
            s.isOpen
        }
    }
}
fun box(): String {
    Bar().Baz()
    return "OK"
}
