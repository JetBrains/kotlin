package a;

import kotlin.Function0;

public class X {
    private A outer;

    public X(A outer, Function0<String> f) {
        this.outer = outer;
        System.out.println(f.invoke());
    }
}