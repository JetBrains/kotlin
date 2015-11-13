package test;

import java.util.*;

public class MethodWithTypeParameters {
    public <A, B extends Runnable & List<Cloneable>> void foo(A a, List<? extends B> b, List<? super String> list) {
    }
}
