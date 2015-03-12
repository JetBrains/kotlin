class JavaUsage {
    public static void main(String[] args) {
        System.out.println(Foo.CONST);
        Foo.s();
        Foo.Bar.f();
        Foo foo = new Foo(); // not usage of default object
    }
}