package test;

public interface HiddenByInstanceField {
    class Base {
        public static final int PUBLIC = 1;
    }

    class Derived extends Base {
        public String PUBLIC = "";
    }
}