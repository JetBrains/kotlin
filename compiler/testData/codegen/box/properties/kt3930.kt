// JVM_ABI_K1_K2_DIFF: KT-63984
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
