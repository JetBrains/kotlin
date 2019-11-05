package test;

public class C {
    public static class D extends C {
        public D() {
            super();
        }

        public static int g() {
            return 1;
        }
    }

    public static D makeD() {
        return new D();
    }
}
