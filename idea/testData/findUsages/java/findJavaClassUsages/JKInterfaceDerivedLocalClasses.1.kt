fun foo() {
    open class X: A

    interface T: A

    fun bar() {
        public open class Y: X()

        public class Z: T
    }
}