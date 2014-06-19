package test;

public class PrivateMembersInHierarchy {
    public static class Super {
        private int field = 1;
        private int field2 = 1;

        private void method() {
        }
    }

    public static class Sub extends Super {
        private int field = 1;

        private void method() {
        }
    }
}

