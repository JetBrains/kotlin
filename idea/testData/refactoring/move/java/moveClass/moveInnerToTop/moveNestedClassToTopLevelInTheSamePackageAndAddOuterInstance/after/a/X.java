package a;

public class X {
    private A outer;

    public X(A outer, String s) {
        this.outer = outer;
        System.out.println(s);
    }
}