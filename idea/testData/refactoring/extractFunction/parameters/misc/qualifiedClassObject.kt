// SIBLING:
class MyClass {
    fun test() {
        <selection>P.foo()
        P.a</selection>
    }

    public class P {
        companion object {
            val a = 1
            fun foo() = 1
        }
    }
}