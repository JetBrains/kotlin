public interface Foo {
    boolean foo();

    class Bar {
        void bar(Foo foo) {}
    }
}