public class A {
    public int foo(int x, String ... args) {
        return x + args.length;
    }

    public static String[] ar = new String[] { "a", "b"};
}