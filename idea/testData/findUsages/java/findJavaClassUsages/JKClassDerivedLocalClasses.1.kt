fun foo() {
    public open class X: A()

    public trait T: A

    fun bar() {
        public open class Y: X()

        public class Z: Y(), T
    }
}