package test;

public class PrivateMembers {
    private PrivateMembers() {
    }

    private int field = 1;
    private static int staticField = 2;

    private void method() {
    }

    private void samAdapter(SamInterface r) {
    }

    private static void staticMethod() {
    }

    private class Inner {
    }

    private interface SamInterface {
        void foo();
    }

    private static class Nested {
        private static void staticMethodInNested() {
        }
    }
}
