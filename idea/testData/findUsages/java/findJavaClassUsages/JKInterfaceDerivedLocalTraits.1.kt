fun foo() {
    open class X: A

    interface T: A

    fun bar() {
        public interface Y: X

        public class Z: T
    }
}