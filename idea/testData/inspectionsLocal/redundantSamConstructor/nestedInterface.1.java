public class Foo {
    public interface Bar {
        void baz();
    }
    public static void foo(Bar bar) {
        bar.baz();
    }
}