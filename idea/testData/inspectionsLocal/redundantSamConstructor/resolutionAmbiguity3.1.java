public interface Interfaces {
    interface FunInterface1 {
        void test();
    }

    interface FunInterface2 {
        void test();
    }

    interface InterfaceWithMethod1 {
        void foo(FunInterface1 f1);
    }

    interface InterfaceWithMethod2 {
        void foo(FunInterface2 f1);
    }
}