package test;

public class PrivateMembers {
    private PrivateMembers() {
    }

    private int field = 1;
    private static int staticField = 2;

    private void method() {
    }

    private static void staticMethod() {
    }

    private class Inner {
    }

    private static class Nested {
        private static void staticMethodInNested() {
        }
    }
}
