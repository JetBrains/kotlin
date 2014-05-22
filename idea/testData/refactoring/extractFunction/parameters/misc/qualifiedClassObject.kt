// SIBLING:
class MyClass {
    fun test() {
        <selection>P.foo()
        P.a</selection>
    }

    public class P {
        class object {
            val a = 1
            fun foo() = 1
        }
    }
}