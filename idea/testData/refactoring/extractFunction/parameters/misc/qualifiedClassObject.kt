// SIBLING:
class MyClass {
    fun test() {
        <selection>P.foo()
        P.a</selection>
    }

    public class P {
        default object {
            val a = 1
            fun foo() = 1
        }
    }
}