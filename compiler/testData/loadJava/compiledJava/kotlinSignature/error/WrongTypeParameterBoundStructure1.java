package test;

import java.util.*;

public class WrongTypeParameterBoundStructure1 {
    public <A, B extends Runnable & List<Cloneable>> void foo(A a, List<? extends B> b) {
    }
}
