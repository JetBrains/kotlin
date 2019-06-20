package my;

public class Reproducer {
    public static Reproducer test() {
        return new Reproducer();
    }

    public int number() {
        return 42;
    }
}