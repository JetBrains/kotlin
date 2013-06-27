package test;

import java.util.List;

public interface GenericMethodParameters {
    <A extends CharSequence, B extends List<A>> void method(A[] a, B b);
}
