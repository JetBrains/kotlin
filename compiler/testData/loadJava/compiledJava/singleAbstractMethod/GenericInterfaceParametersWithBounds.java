package test;

import java.util.List;

public interface GenericInterfaceParametersWithBounds<A extends Comparable<A> & Cloneable, B extends List<A>> {
    void method(A[] a, B b);
}
