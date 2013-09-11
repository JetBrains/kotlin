package test;

public class OverrideMethod {
    class SuperBase {
        void quux(int x) {}
    }

    class Base extends SuperBase {
        String foo(String s) {
            return s;
        }

        void bar() {}
    }

    class Derived extends Base {
        @Override
        String foo(String s) {
            return null;
        }

        void baz() {}
    }
}
