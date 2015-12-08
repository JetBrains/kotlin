class B extends A {
    void bar() {
        System.out.println(getFoo());
    }

    @Override
    public String getFoo() {
        return "foo";
    }
}