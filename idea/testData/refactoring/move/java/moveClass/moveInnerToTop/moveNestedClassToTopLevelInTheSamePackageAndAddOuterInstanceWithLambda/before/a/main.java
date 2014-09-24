package a;

import kotlin.Function0;

public class A {
    public class <caret>X {
        public X(Function0<String> f) {
            System.out.println(f.invoke());
        }
    }
}