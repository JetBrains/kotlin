class WithPrimary {
    public static A test1() {
        return new A("123", "abc");
    }
    public static A test2() {
        return new A();
    }
    public static A test3() {
        return new A("123", 456);
    }
    public static A test4() {
        return new A(1.0);
    }
}
