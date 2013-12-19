fun foo() {
    open class X: A

    trait T: A

    fun bar() {
        public open class Y: X()

        public class Z: T
    }
}