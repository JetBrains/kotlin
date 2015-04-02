package test;

public enum EnumWithSpecializedEntry {
    E1,

    E2 {
        String foo() {
            return name();
        }
    };

    static class Nested {}
}
