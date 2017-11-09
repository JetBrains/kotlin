package a;

import a.impl.AImpl;

public class A {
    public static AImpl getInstance() {
        return new AImpl();
    }
}
