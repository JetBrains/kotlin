public class Foo {
    public static class Bar {
    }

    public Bar getBar1() {
        return null;
    }

    public Bar getBar2() {
        return new Bar();
    }
}
