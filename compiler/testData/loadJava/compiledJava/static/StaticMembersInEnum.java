package test;

public enum StaticMembersInEnum {
    ENTRY;

    public static void foo() { }
    public static void values(int x) { }
    public static void valueOf(int x) { }
    
    public static int STATIC_FIELD = 42;
    public static final StaticMembersInEnum CONSTANT = ENTRY;
}
