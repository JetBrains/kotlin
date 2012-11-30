package test;

import java.util.Collection;

public interface RemoveRedundantProjectionKind {
    void f(Collection<? extends CharSequence> collection);
    void f(Comparable<? super CharSequence> comparator);
}
