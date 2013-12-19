fun foo() {
    open class X: A

    trait T: A

    fun bar() {
        public trait Y: X

        public class Z: T
    }
}