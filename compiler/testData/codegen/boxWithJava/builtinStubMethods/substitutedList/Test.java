class Test {
    interface A {
        boolean add(String s);
    }

    static class D extends C {}

    void test() {
        A a = new D();
        a.add("lol");
    }
}
